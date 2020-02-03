package com.bootapp.core.service;

import com.bootapp.core.config.Constants;
import com.bootapp.core.domain.*;
import com.bootapp.grpc.core.CoreCommon;
import com.bootapp.grpc.core.DalUser;
import com.bootapp.core.repository.*;
import com.bootapp.core.utils.CommonUtils;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import com.bootapp.core.utils.idgen.IDGenerator;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserOrgsRepository userOrgsRepository;
    private final RelationsRepository relationsRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final MessageRepository messageRepository;
    private final RoleOrgRepository roleOrgRepository;
    private final RoleUserRepository roleUserRepository;
    private final InboxRepository inboxRepository;
    private final DictItemRepository dictItemRepository;
    private final IDGenerator idGen;
    private final FeedbackRepository feedbackRepository;
    private JPAQueryFactory queryFactory;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserService(UserRepository userRepository, UserOrgsRepository userOrgsRepository, RelationsRepository relationsRepository, OrganizationRepository organizationRepository, DepartmentRepository departmentRepository, MessageRepository messageRepository, RoleOrgRepository roleOrgRepository, RoleUserRepository roleUserRepository, InboxRepository inboxRepository, DictItemRepository dictItemRepository, IDGenerator idGen, FeedbackRepository feedbackRepository, EntityManager em) {
        this.userRepository = userRepository;
        this.userOrgsRepository = userOrgsRepository;
        this.relationsRepository = relationsRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.messageRepository = messageRepository;
        this.roleOrgRepository = roleOrgRepository;
        this.roleUserRepository = roleUserRepository;
        this.inboxRepository = inboxRepository;
        this.dictItemRepository = dictItemRepository;
        this.idGen = idGen;
        this.feedbackRepository = feedbackRepository;
        queryFactory = new JPAQueryFactory(em);
    }

    @Transactional
    public CoreCommon.UserWithOrgAuth.Builder saveUser(DalUser.CreateUserReq req) {
        User user = new User();
        user.fromProto(req.getUser());
        //------------ set password
        if (req.getUser().hasPassword())
            user.setPasswordHash(BCrypt.hashpw(req.getUser().getPassword().getValue(), BCrypt.gensalt()));
        user.setId(idGen.nextId());
        logger.info("new user saving to db with id: {}", user.getId());
        UserOrgs userOrgs = new UserOrgs(idGen.nextId(), user.getId(), 1, 1);
        userRepository.save(user);
        userOrgsRepository.save(userOrgs);
        CoreCommon.UserWithOrgAuth.Builder resp = CoreCommon.UserWithOrgAuth.newBuilder();
        resp.setUser(user.toProto());
        return resp;
    }

    @Transactional
    public CoreCommon.UsersResp.Builder createUsers(DalUser.CreateUsersReq request) {
        List<User> users = request.getDataList().stream().map(x -> {
            User newUser = new User();
            newUser.fromProto(x);
            newUser.setId(idGen.nextId());
            return newUser;
        }).collect(Collectors.toList());
        userRepository.saveAll(users);
        List<UserOrgs> userOrgs = users.stream().map(x -> new UserOrgs(idGen.nextId(), x.getId(), 1, request.getRoleId())).collect(Collectors.toList());
        userOrgsRepository.saveAll(userOrgs);
        return CoreCommon.UsersResp.newBuilder().addAllData(
                users.stream().map(User::toProto).collect(Collectors.toList())
        );
    }

    public CoreCommon.UserWithOrgAuth.Builder readUserAuth(DalUser.ReadUserReq req) {
        CoreCommon.UserWithOrgAuth.Builder resp = CoreCommon.UserWithOrgAuth.newBuilder();
        QUser qUser = QUser.user;

        BooleanExpression queryExpressions = null;
        if (req.getId() != 0L)
            queryExpressions = qUser.id.eq(req.getId());
        else {
            if (req.getUsername() != null && !req.getUsername().equals(""))
                queryExpressions = qUser.username.eq(req.getUsername());
            if (req.getEmail() != null && !req.getEmail().equals(""))
                queryExpressions = qUser.email.eq(req.getEmail()).or(queryExpressions);
            if (req.getPhone() != null && !req.getPhone().equals(""))
                queryExpressions = qUser.phone.eq(req.getPhone()).or(queryExpressions);
        }
        if (queryExpressions == null) throw GrpcStatusException.GrpcNotFoundException();
        User dbUser = queryFactory.selectFrom(qUser).where(queryExpressions).fetchOne();
        if (dbUser == null) throw GrpcStatusException.GrpcNotFoundException();

        if (dbUser.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE)
            throw GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:inactivated");
        if (req.getId() == 0 && !req.getPassword().equals("") && !BCrypt.checkpw(req.getPassword(), dbUser.getPasswordHash()))
            throw GrpcStatusException.GrpcInvalidArgException("INVALID_PASSWORD");
        resp.setUser(dbUser.toProto());
        List<UserOrgs> userOrgsList;
        if (req.getOrgId() != 0) {
            Optional<UserOrgs> userOrgs = userOrgsRepository.findOneByUserIdAndOrgId(dbUser.getId(), req.getOrgId());
            if (!userOrgs.isPresent())
                throw GrpcStatusException.GrpcInternalException("NON_EXISTS");
            userOrgsList = Collections.singletonList(userOrgs.get());
        } else {
            userOrgsList = userOrgsRepository.findAllByUserId(dbUser.getId());
        }
        // return error if user has no organization,
        // remove the default org if the user has more than one organization.
        if (userOrgsList.size() == 0) {
            throw GrpcStatusException.GrpcInternalException("NON_EXISTS");
        } else if (userOrgsList.size() > 1) {
            for (int i = 0; i < userOrgsList.size(); i++) {
                if (userOrgsList.get(i).getOrgId() == 1) {
                    userOrgsList.remove(i);
                    break;
                }
            }
        }
        // todo: remove user authorities when org auth revoked.
        userOrgsList.forEach(x -> {
            CoreCommon.OrgWithAuthorities.Builder userOrgBuilder = CoreCommon.OrgWithAuthorities.newBuilder();
            Organization organization = organizationRepository.findById(x.getOrgId()).get();
            RoleOrg roleOrg = roleOrgRepository.findById(organization.getOrgRoleId()).get();
            RoleUser roleUser = roleUserRepository.findById(x.getUserRoleId()).get();
            userOrgBuilder.setId(x.getOrgId());
            if (organization.getName() != null) userOrgBuilder.setName(organization.getName());
            if (roleUser.getAuthorities() != null) userOrgBuilder.setAuthorities(roleUser.getAuthorities());
            if (roleOrg.getAuthorities() != null) userOrgBuilder.setAuthorityGroups(roleOrg.getAuthorities());
            resp.addOrgInfo(userOrgBuilder.buildPartial());
        });
        return resp;
    }

    public CoreCommon.UsersResp.Builder readUsers(DalUser.ReadUsersReq req) {
        CoreCommon.UsersResp.Builder resp = CoreCommon.UsersResp.newBuilder();
        QUser qUser = QUser.user;
        BooleanExpression expr = null;
        QueryResults<User> qRes;

        if (req.getIdsCount() > 0) {
            expr = qUser.id.in(req.getIdsList());
            qRes = queryFactory.selectFrom(qUser).where(expr).fetchResults();
        } else {
            long size = req.getPagination().getSize();
            if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
            if (!req.getUsername().equals("")) expr = qUser.username.startsWith(req.getUsername());
            if (!req.getEmail().equals("")) expr = qUser.email.startsWith(req.getEmail()).or(expr);
            if (!req.getPhone().equals("")) {
                String qStr;
                if (!req.getPhone().startsWith("+")) qStr = "+" + req.getPhone();
                else qStr = req.getPhone();
                qStr = qStr.replace(" ", "").replace("+", "/+").replace("-", "/-") + "%";
                expr = qUser.phone.like(qStr, '/').or(expr);
            }
            if (req.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL) expr = qUser.status.eq(req.getStatusValue()).and(expr);
            qRes = queryFactory.selectFrom(qUser)
                    .where(expr).offset(req.getPagination().getIdx()).limit(size).fetchResults();
            resp.setPagination(CoreCommon.Pagination.newBuilder()
                    .setTotal(qRes.getTotal()).setIdx(qRes.getOffset()).setSize(size));
        }
        qRes.getResults().forEach(it -> resp.addData(it.toProto()));
        return resp;
    }
    @Transactional
    public void updateUser(DalUser.UpdateUserReq request) {
        Optional<User> user;
        switch (request.getType()) {
            case UPDATE_USER_TYPE_ID:
                user = userRepository.findById(request.getUser().getId());
                break;
            case UPDATE_USER_TYPE_USERNAME:
                user = userRepository.findOneByUsername(request.getUser().getUsername().getValue());
                break;
            case UPDATE_USER_TYPE_PHONE:
                user = userRepository.findOneByPhone(request.getUser().getPhone().getValue());
                break;
            case UPDATE_USER_TYPE_EMAIL:
                user = userRepository.findOneByEmail(request.getUser().getEmail().getValue());
                break;
            default:
                user = null;
                break;
        }
        if (user == null || !user.isPresent()) throw GrpcStatusException.GrpcNotFoundException();
        User dbUser = user.get();
        dbUser.fromProto(request.getUser());
        //------------ set password
        if (request.getUser().hasPassword())
            dbUser.setPasswordHash(BCrypt.hashpw(request.getUser().getPassword().getValue(), BCrypt.gensalt()));
        userRepository.save(dbUser);
    }

    public void verifyUniqueUser(CoreCommon.User request) {
        if (!request.getUsername().equals("")) {
            if (userRepository.findOneByUsername(request.getUsername()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:username");
        }
        if (!request.getPhone().equals("")) {
            if (userRepository.findOneByPhone(request.getPhone()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:phone");
        }
        if (!request.getEmail().equals("")) {
            if (userRepository.findOneByEmail(request.getEmail()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:email");
        }
    }

    public DalUser.IdsResp readUserIds(CoreCommon.AuthorizedPaginationReq request) {
        DalUser.IdsResp.Builder resp = DalUser.IdsResp.newBuilder();
        QUser qU = QUser.user;
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
        QueryResults<Long> ids = queryFactory.select(qU.id).from(qU)
                .offset(request.getPagination().getIdx()).limit(size).fetchResults();
        resp.addAllIds(ids.getResults());
        resp.setPagination(CommonUtils.buildPagination(ids.getOffset(), ids.getLimit(), ids.getTotal()));
        return resp.build();
    }
    @Transactional
    public void updateOrgs(DalUser.OrgsReq request) {
        Map<Long, CoreCommon.OrganizationEdit> orgMap = new HashMap<>();
        List<Organization> orgsToSave = new ArrayList<>();
        request.getDataList().forEach(x -> {
            if (x.getId() == 0L) {
                Organization org = new Organization();
                org.setId(idGen.nextId());
                org.fromProto(x);
                orgsToSave.add(org);
            } else {
                orgMap.put(x.getId(), x);
            }
        });
        if (orgMap.size() > 0) {
            List<Organization> orgs = organizationRepository.findAllById(orgMap.keySet());
            orgs.forEach(x -> {
                if (!orgMap.containsKey(x.getId())) return;
                x.fromProto(orgMap.get(x.getId()));
                orgsToSave.add(x);
            });
        }
        organizationRepository.saveAll(orgsToSave);
    }

    public CoreCommon.OrgsResp.Builder readOrgs(DalUser.ReadOrgsReq request) {
        QOrganization qOrganization = QOrganization.organization;
        CoreCommon.OrganizationEdit q = request.getOrg();
        BooleanExpression query = qOrganization.status.ne(CoreCommon.EntityStatus.ENTITY_STATUS_DELETED_VALUE);
        if (q.hasName()) query = qOrganization.name.like(q.getName().getValue() + "%").and(query);
        if (q.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
            query = qOrganization.status.eq(q.getStatusValue()).and(query);
        if (q.hasCode()) query = qOrganization.code.like(q.getCode().getValue() + "%").and(query);
        if (q.hasOrgRoleId()) qOrganization.orgRoleId.eq(q.getOrgRoleId().getValue()).and(query);
        QueryResults<Organization> orgs;
        CoreCommon.Pagination pagination = request.getPagination();
        long limit = pagination.getSize();
        if (limit <= 0) limit = Constants.DEFAULT_PAGINATION_SIZE;
        switch (pagination.getSort()) {
            case SORT_TYPE_TIME_DESC:
            case SORT_TYPE_ID_DESC:
                orgs = queryFactory.selectFrom(qOrganization).where(query).orderBy(qOrganization.id.desc())
                        .offset(pagination.getIdx()).limit(limit).fetchResults();
                break;
            default:
                orgs = queryFactory.selectFrom(qOrganization).where(query).orderBy(qOrganization.id.asc())
                        .offset(pagination.getIdx()).limit(limit).fetchResults();
                break;
        }
        CoreCommon.OrgsResp.Builder resp = CoreCommon.OrgsResp.newBuilder();
        resp.addAllData(orgs.getResults().stream().map(Organization::toProto).collect(Collectors.toList()));
        resp.setPagination(CommonUtils.buildPagination(orgs.getOffset(), orgs.getLimit(), orgs.getTotal()));
        return resp;
    }
    @Transactional
    public void updateDepts(DalUser.DeptsReq request) {
        Map<Long, CoreCommon.DepartmentEdit> deptMap = new HashMap<>();
        List<Department> deptsToSave = new ArrayList<>();
        request.getDataList().forEach(x -> {
            if (x.getId() == 0L) {
                Department department = new Department();
                department.setId(idGen.nextId());
                department.fromProto(x);
                if (department.getOrgId() == 0L) department.setOrgId(request.getOrgId());
                if (department.getStatus() == CoreCommon.EntityStatus.ENTITY_STATUS_NULL_VALUE)
                    department.setStatus(CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE);
                deptsToSave.add(department);
            } else {
                deptMap.put(x.getId(), x);
            }
        });
        if (deptMap.size() > 0) {
            List<Department> departments = departmentRepository.findAllById(deptMap.keySet());
            departments.forEach(x-> {
                if (!deptMap.containsKey(x.getId())) return;
                x.fromProto(deptMap.get(x.getId()));
                deptsToSave.add(x);
            });
        }
        departmentRepository.saveAll(deptsToSave);
    }
    public CoreCommon.DeptsResp.Builder readDepts(DalUser.ReadDeptsReq request) {
        QDepartment qDepartment = QDepartment.department;
        BooleanExpression query = qDepartment.status.ne(CoreCommon.EntityStatus.ENTITY_STATUS_DELETED_VALUE);
        CoreCommon.DepartmentEdit q = request.getDept();
        if (q.hasName()) query = qDepartment.name.like(q.getName().getValue() + "%").and(query);
        if (q.getOrgId() != 0L) query = qDepartment.orgId.eq(q.getOrgId()).and(query);
        if (q.getPid() != 0L) query = qDepartment.pid.eq(q.getPid()).and(query);
        if (q.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
            query = qDepartment.status.eq(q.getStatusValue()).and(query);
        QueryResults<Department> results;
        CoreCommon.Pagination pagination = request.getPagination();
        long limit = pagination.getSize();
        if (limit <= 0) limit = Constants.DEFAULT_PAGINATION_SIZE;
        switch (pagination.getSort()) {
            case SORT_TYPE_ID_DESC:
            case SORT_TYPE_TIME_DESC:
                results = queryFactory.selectFrom(qDepartment).where(query).orderBy(qDepartment.id.desc())
                        .offset(pagination.getIdx()).limit(limit).fetchResults();
                break;
            default:
                results = queryFactory.selectFrom(qDepartment).where(query).orderBy(qDepartment.id.asc())
                        .offset(pagination.getIdx()).limit(limit).fetchResults();
                break;
        }
        CoreCommon.DeptsResp.Builder resp = CoreCommon.DeptsResp.newBuilder();
        resp.addAllData(results.getResults().stream().map(Department::toProto).collect(Collectors.toList()));
        resp.setPagination(CommonUtils.buildPagination(results.getOffset(), results.getLimit(), results.getTotal()));
        return resp;
    }
    @Transactional
    public void updateMessage(DalUser.MessageReq request) {
        Message msg;
        if (request.getMsg().getId() == 0L) {
            msg = new Message();
            msg.setId(idGen.nextId());
            msg.setStatus(CoreCommon.EntityStatus.ENTITY_STATUS_SUBMITTED_VALUE);
        } else {
            Optional<Message> msgOptional = messageRepository.findById(request.getMsg().getId());
            if (msgOptional.isPresent()) {
                msg = msgOptional.get();
            } else {
                throw GrpcStatusException.GrpcNotFoundException();
            }
        }
        msg.fromProto(request.getMsg());
        messageRepository.save(msg);
    }

    public CoreCommon.MessageList readMessages(DalUser.ReadMessagesReq request) {
        CoreCommon.MessageList.Builder builder = CoreCommon.MessageList.newBuilder();
        QMessage qMsg = QMessage.message;
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
        BooleanExpression qe = null;

        // ===== msg type
        if (request.getType() != CoreCommon.MessageType.MESSAGE_TYPE_NULL)
            qe = qMsg.type.eq(request.getTypeValue());

        // ===== receive type
        if (request.getReceiveType() != CoreCommon.ReceiveType.RECEIVE_TYPE_NULL)
            qe = qMsg.receiveType.eq(request.getReceiveTypeValue()).and(qe);

        // ===== title
        if (!request.getTitle().equals(""))
            qe = qMsg.title.startsWith(request.getTitle()).and(qe);

        // ===== content
        if (!request.getContent().equals(""))
            qe = qMsg.content.startsWith(request.getContent()).and(qe);

        if (request.getFromUserId() != 0L)
            qe = qMsg.userId.eq(request.getFromUserId()).and(qe);

        if (request.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
            qe = qMsg.status.eq(request.getStatusValue()).and(qe);

        if (request.getPagination().getIdx() != 0L) {
            qe = qMsg.id.lt(request.getPagination().getIdx());
        }

        List<Message> msgs = queryFactory.selectFrom(qMsg).where(qe).orderBy(qMsg.id.desc()).limit(size).fetch();
        builder.addAllData(msgs.stream().map(Message::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    @Transactional
    public void createInbox(DalUser.CreateInboxReq request) {
        List<Inbox> inboxToSave = new ArrayList<>();
        request.getToUserIdsList().forEach(x -> {
            Inbox inbox = new Inbox();
            inbox.setId(idGen.nextId());
            inbox.setUserId(x);
            inbox.setMsgId(request.getMsgId());
            inboxToSave.add(inbox);
        });
        inboxRepository.saveAll(inboxToSave);
    }

    public void updateInbox(DalUser.UpdateInboxReq request) {
        List<Inbox> inboxes = inboxRepository.findAllById(request.getIdsList());
        inboxes.forEach(x -> {
            if (x.getUserId() != request.getUserId())
                throw GrpcStatusException.GrpcUnauthorizedException();
            x.setStatus(request.getStatusValue());
        });
        inboxRepository.saveAll(inboxes);
    }
    public CoreCommon.MessageList readInbox(DalUser.ReadInboxReq request) {
        CoreCommon.MessageList.Builder resp = CoreCommon.MessageList.newBuilder();
        QInbox qI = QInbox.inbox;
        QMessage qMsg = QMessage.message;
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
        BooleanExpression expr = qI.userId.eq(request.getQueryUserId());
        if (request.getPagination().getIdx() != 0)
            expr = qI.id.lt(request.getPagination().getIdx()).and(expr);

        List<Tuple> msgs = queryFactory.select(qMsg, qI).from(qI).leftJoin(qMsg).on(qMsg.id.eq(qI.msgId))
                .where(expr).orderBy(qI.id.desc()).limit(size).fetch();
        List<CoreCommon.Message> msgRes = new ArrayList<>();
        msgs.forEach(x -> {
            Message msg = x.get(qMsg);
            Inbox inbox = x.get(qI);
            if (msg != null && inbox != null) {
                CoreCommon.Message.Builder builder = msg.toProtoBuilder();
                builder.setStatusValue(inbox.getStatus());
                msgRes.add(builder.build());
            }
        });
        resp.addAllData(msgRes);
        return resp.build();
    }

    @Transactional
    public void deleteInbox(DalUser.UpdateInboxReq request) {
        inboxRepository.deleteByIdIn(request.getIdsList());
    }
    @Transactional
    public void updateRelations(DalUser.UpdateRelationsReq request) {
        Map<Long, CoreCommon.Relation> relationMap = new HashMap<>();
        List<Relation> relationsToSave = new ArrayList<>();
        request.getRelationList().forEach(x -> {
            if (x.getId() == 0L) {
                Relation relat = new Relation();
                relat.setId(idGen.nextId());
                relat.fromProto(x);
                if (relat.getStatus() == CoreCommon.EntityStatus.ENTITY_STATUS_NULL_VALUE)
                    relat.setStatus(CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE);
                relationsToSave.add(relat);
            } else {
                relationMap.put(x.getId(), x);
            }
        });
        if (relationMap.size() > 0) {
            List<Relation> relations = relationsRepository.findAllById(relationMap.keySet());
            relations.forEach(x -> {
                if (!relationMap.containsKey(x.getId())) return;
                x.fromProto(relationMap.get(x.getId()));
                relationsToSave.add(x);
            });
        }

        relationsRepository.saveAll(relationsToSave);
    }
    @Transactional
    public void deleteRelations(CoreCommon.AuthorizedIdsReq request) {
        relationsRepository.deleteByIdIn(request.getIdsList());
    }

    public QueryResults<Relation> queryRelations(CoreCommon.Relation req, long idx, long size) {
        QRelation qRelations = QRelation.relation;
        BooleanExpression exp = null;
        if (req.getType() != CoreCommon.RelationType.RELATION_NULL) exp = qRelations.relationType.eq(req.getTypeValue());
        if (req.getSourceId() != 0L) exp = qRelations.sourceId.eq(req.getSourceId()).and(exp);
        if (req.getTargetId() != 0L) exp = qRelations.targetId.eq(req.getTargetId()).and(exp);
        if (req.getAttachedId1() != 0L) exp = qRelations.attachedId1.eq(req.getAttachedId1()).and(exp);
        if (req.getAttachedId2() != 0L) exp = qRelations.attachedId2.eq(req.getAttachedId2()).and(exp);
        if (req.hasValue1()) exp = qRelations.value1.like(req.getValue1().getValue() + "%").and(exp);
        if (req.hasValue2()) exp = qRelations.value2.like(req.getValue2().getValue() + "%").and(exp);
        if (req.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL) exp = qRelations.status.eq(req.getStatusValue()).and(exp);
        return queryFactory.selectFrom(qRelations).where(exp)
                .offset(idx).limit(size).fetchResults();
    }
    public CoreCommon.PartnersResp.Builder readPartners(DalUser.ReadPartnersReq request) {
        CoreCommon.PartnersResp.Builder builder = CoreCommon.PartnersResp.newBuilder();
        long size = request.getPagination().getSize();
        if (size == 0L) size = Constants.DEFAULT_PAGINATION_SIZE;

        QRelation qRelation = QRelation.relation;
        QOrganization qOrganization = QOrganization.organization;
        QUser qUser = QUser.user;
        QUserOrgs qUserOrgs = QUserOrgs.userOrgs;
        BooleanExpression query = qRelation.sourceId.eq(request.getOrgId());
        if (!request.getOrgSerialNumber().equals("")) query = qOrganization.code.like(request.getOrgSerialNumber() + "%");
        if (!request.getOrgName().equals("")) query = qOrganization.name.like(request.getOrgName() + "%").or(query);
        if (!request.getContactPhone().equals("")) query = qUser.phone.like(request.getContactPhone() + "%").or(query);
        if (!request.getContactEmail().equals("")) query = qUser.email.like(request.getContactEmail() + "%").or(query);
        QueryResults<Relation> relations = queryFactory.selectFrom(qRelation)
                .leftJoin(qUser).on(qUser.id.eq(qRelation.targetId))
                .leftJoin(qUserOrgs).on(qUserOrgs.userId.eq(qRelation.targetId))
                .leftJoin(qOrganization).on(qOrganization.id.eq(qUserOrgs.orgId))
                .where(query).offset(request.getPagination().getIdx()).limit(size).distinct().fetchResults();

        builder.addAllRelations(relations.getResults().stream().map(Relation::toProto).collect(Collectors.toList()));
        builder.setPagination(CommonUtils.buildPagination(relations.getOffset(), relations.getLimit(), relations.getTotal()));

        // ************************************* 根据查询结果组织user、org信息
        if (relations.getResults().size() > 0) {
            List<User> userList = new ArrayList<>();
            List<Long> userIds = new ArrayList<>();
            Map<Long, Long> userOrgMap = new HashMap<>();

            for (Relation r : relations.getResults()) userIds.add(r.getTargetId());

            List<Tuple> userOrgRes = queryFactory.select(qUser, qUserOrgs).from(qUserOrgs)
                    .leftJoin(qUser).on(qUser.id.eq(qUserOrgs.userId))
                    .where(qUserOrgs.userId.in(userIds)).fetch();

            for (Tuple tuple : userOrgRes) {
                User user = tuple.get(qUser);
                UserOrgs org = tuple.get(qUserOrgs);
                if (user != null) {
                    userList.add(user);
                    if (org != null && org.getOrgId() != 1L) userOrgMap.put(user.getId(), org.getOrgId());
                }
            }
            List<Organization> organizations = organizationRepository.findAllById(userOrgMap.values());
            // todo possible multiple organizations
            Map<Long, Organization> orgMap = organizations.stream().collect(Collectors.toMap(Organization::getId, x->x));
            for(User user : userList) {
                CoreCommon.UserWithOrgInfo.Builder userOrgBuilder = CoreCommon.UserWithOrgInfo.newBuilder();
                userOrgBuilder.setUser(user.toProto());
                if (userOrgMap.containsKey(user.getId()) && orgMap.containsKey(userOrgMap.get(user.getId()))) {
                    userOrgBuilder.addOrgs(orgMap.get(userOrgMap.get(user.getId())).toProto());
                }
                builder.putUserMap(user.getId(), userOrgBuilder.build());
            }
        }
        return builder;
    }
    public CoreCommon.PartnersResp.Builder readPartnersIn(DalUser.ReadPartnersInReq request) {
        CoreCommon.PartnersResp.Builder builder = CoreCommon.PartnersResp.newBuilder();
        QRelation qRelations = QRelation.relation;
        List<Relation> relationList = queryFactory.selectFrom(qRelations).where(
                qRelations.sourceId.eq(request.getOrgId())
                        .and(qRelations.targetId.in(request.getPartnerIdsList()))).fetch();
        builder.addAllRelations(relationList.stream().map(Relation::toProto).collect(Collectors.toList()));
        return builder;
    }

    @Transactional
    public void updateUserOrgs(DalUser.CreateUserOrgsReq request) {
        List<UserOrgs> userOrgsList = new ArrayList<>();
        request.getDataList().forEach(x -> {
            Optional<UserOrgs> userOrgsOptional = userOrgsRepository.findOneByUserIdAndOrgId(x.getUserId(), x.getOrgId());
            UserOrgs userOrgs;
            if (userOrgsOptional.isPresent()) {
                userOrgs = userOrgsOptional.get();

            } else {
                userOrgs = new UserOrgs();
                userOrgs.setId(idGen.nextId());
                userOrgs.setUserId(x.getUserId());
                userOrgs.setOrgId(x.getOrgId());
            }
            userOrgs.setUserRoleId(x.getUserRoleId());
            userOrgsList.add(userOrgs);
        });
        userOrgsRepository.saveAll(userOrgsList);
    }

    @Transactional
    public void createOrgAndUserOrg(DalUser.CreateOrgUserOrgReq request) {
        Organization org = new Organization();
        UserOrgs userOrgs = new UserOrgs();
        org.fromProto(request.getOrg());
        org.setStatus(CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE);
        org.setId(idGen.nextId());
        userOrgs.setUserId(request.getUserId());
        userOrgs.setOrgId(org.getId());
        userOrgs.setUserRoleId(request.getUserRoleId());
        organizationRepository.save(org);
        userOrgsRepository.save(userOrgs);

    }
    public DalUser.UserOrgsResp.Builder readUserOrgs(CoreCommon.AuthorizedReq request) {
        DalUser.UserOrgsResp.Builder builder = DalUser.UserOrgsResp.newBuilder();
        QUserOrgs qUserOrgs = QUserOrgs.userOrgs;
        BooleanExpression query = qUserOrgs.userId.eq(request.getUserId()).and(qUserOrgs.status.ne(CoreCommon.EntityStatus.ENTITY_STATUS_DELETED_VALUE));
        if (request.getOrgId() == 0L) {
            query = query.and(qUserOrgs.orgId.ne(1L));
        } else {
            query = query.and(qUserOrgs.orgId.eq(request.getOrgId()));
        }
        List<UserOrgs> res = queryFactory.selectFrom(qUserOrgs).where(query).fetch();
        builder.addAllData(res.stream().map(UserOrgs::toProto).collect(Collectors.toList()));
        return builder;
    }

    @Transactional
    public void updateDictItems(DalUser.DictItemsReq request) {
        if (request.getDataCount() > 0) {
            List<DictItem> itemsToSave = new ArrayList<>();
            Map<Long, CoreCommon.DictItemEdit> itemMap = new HashMap<>();
            for(CoreCommon.DictItemEdit item : request.getDataList()) {
                if (item.getId() != 0L)
                    itemMap.put(item.getId(), item);
                else {
                    DictItem dItem = new DictItem();
                    dItem.fromProto(item);
                    dItem.setId(idGen.nextId());
                    itemsToSave.add(dItem);
                }
            }
            if (itemMap.size() > 0) {
                List<DictItem> dictItems = dictItemRepository.findAllById(itemMap.keySet());
                dictItems.forEach(x -> {
                    x.fromProto(itemMap.get(x.getId()));
                    itemsToSave.add(x);
                });
            }
            dictItemRepository.saveAll(itemsToSave);
        }
    }

    public CoreCommon.DictItemList readDictItems(DalUser.ReadDictItemsReq request) {
        CoreCommon.DictItemList.Builder builder = CoreCommon.DictItemList.newBuilder();
        QDictItem qDict = QDictItem.dictItem;
        List<DictItem> dictItems;
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;

        if (request.getIdsCount() > 0) {
            dictItems = queryFactory.selectFrom(qDict).where(qDict.id.in(request.getIdsList())).fetch();
        } else {
            BooleanExpression expr = null;
            if (request.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
                expr = qDict.status.eq(request.getStatusValue());
            if (request.getPid() != 0) expr = qDict.pid.eq(request.getPid()).and(expr);

            QueryResults<DictItem> qR = queryFactory.selectFrom(qDict).where(expr).orderBy(qDict.seq.asc())
                    .offset(request.getPagination().getIdx()).limit(size).fetchResults();
            dictItems = qR.getResults();
            builder.setPagination(CommonUtils.buildPagination(qR.getOffset(), qR.getLimit(), qR.getTotal()));
        }
        builder.addAllData(dictItems.stream().map(DictItem::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    @Transactional
    public void deleteDictItems(CoreCommon.AuthorizedIdsReq request) {
        dictItemRepository.deleteByIdIn(request.getIdsList());
    }

    @Transactional
    public void updateFeedback(DalUser.FeedbackReq request) {
        Feedback item;
        if (request.getItem().getId() == 0) {
            item = new Feedback();
            item.setId(idGen.nextId());
        } else {
            Optional<Feedback> feedbackOptional = feedbackRepository.findById(request.getItem().getId());
            if (feedbackOptional.isPresent()) {
                item = feedbackOptional.get();
            } else {
                throw GrpcStatusException.GrpcNotFoundException();
            }
        }
        item.fromProto(request.getItem());
        feedbackRepository.save(item);
    }

    public CoreCommon.FeedbackList readFeedback(DalUser.ReadFeedbackReq request) {
        CoreCommon.FeedbackList.Builder resp = CoreCommon.FeedbackList.newBuilder();
        QFeedback qF = QFeedback.feedback;
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;

        BooleanExpression expr = null;
        QueryResults<Feedback> data;
        if (request.getId() != 0L) {
            expr = qF.id.eq(request.getId());
            data = queryFactory.selectFrom(qF).where(expr).fetchResults();
        } else {
            if (request.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
                expr = qF.status.eq(request.getStatusValue());
            data = queryFactory.selectFrom(qF).where(expr)
                    .offset(request.getPagination().getIdx()).limit(size).fetchResults();
        }

        resp.addAllData(data.getResults().stream().map(Feedback::toProto).collect(Collectors.toList()));
        resp.setPagination(CommonUtils.buildPagination(data.getOffset(), data.getLimit(), data.getTotal()));
        return resp.build();
    }
}
