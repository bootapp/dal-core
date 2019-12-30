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
    private final RelationVisitRepository relationVisitRepository;
    private final RelationFollowRepository relationFollowRepository;
    private final RelationBlacklistRepository relationBlacklistRepository;
    private final InboxRepository inboxRepository;
    private final DictItemRepository dictItemRepository;
    private final IDGenerator idGen;
    private JPAQueryFactory queryFactory;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserService(UserRepository userRepository, UserOrgsRepository userOrgsRepository, RelationsRepository relationsRepository, OrganizationRepository organizationRepository, DepartmentRepository departmentRepository, MessageRepository messageRepository, RoleOrgRepository roleOrgRepository, RoleUserRepository roleUserRepository, RelationVisitRepository relationVisitRepository, RelationFollowRepository relationFollowRepository, RelationBlacklistRepository blacklistRepository, InboxRepository inboxRepository, DictItemRepository dictItemRepository, IDGenerator idGen, EntityManager em) {
        this.userRepository = userRepository;
        this.userOrgsRepository = userOrgsRepository;
        this.relationsRepository = relationsRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.messageRepository = messageRepository;
        this.roleOrgRepository = roleOrgRepository;
        this.roleUserRepository = roleUserRepository;
        this.relationVisitRepository = relationVisitRepository;
        this.relationFollowRepository = relationFollowRepository;
        this.relationBlacklistRepository = blacklistRepository;
        this.inboxRepository = inboxRepository;
        this.dictItemRepository = dictItemRepository;
        this.idGen = idGen;
        queryFactory = new JPAQueryFactory(em);
    }

    @Transactional
    public CoreCommon.UserWithOrgAuth.Builder saveUser(DalUser.CreateUserReq request) {
        CoreCommon.User req = request.getUser();
        User user = new User();
        user.fromProto(req);
        //------------ set password
        if (req.hasPassword())
            user.setPasswordHash(BCrypt.hashpw(req.getPassword().getValue(), BCrypt.gensalt()));
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

    @Transactional
    public CoreCommon.UsersResp.Builder createUsersWithOrg(DalUser.CreateUsersWithOrgReq request) {
        List<Organization> orgs = new ArrayList<>();
        List<UserOrgs> userOrgs = new ArrayList<>();
        List<User> users = request.getDataList().stream().map(x -> {
            User newUser = new User();
            newUser.fromProto(x.getUser());
            newUser.setId(idGen.nextId());
            Organization org = new Organization();
            org.fromProto(x.getOrg());
            org.setId(idGen.nextId());
            org.setOrgRoleId(1L);
            if (org.getStatus() == CoreCommon.EntityStatus.ENTITY_STATUS_NULL_VALUE)
                org.setStatus(CoreCommon.EntityStatus.ENTITY_STATUS_INACTIVATED_VALUE);
            orgs.add(org);
            UserOrgs userOrg = new UserOrgs(idGen.nextId(), newUser.getId(),
                    org.getId(), request.getRoleId());
            userOrgs.add(userOrg);
            return newUser;
        }).collect(Collectors.toList());
        userRepository.saveAll(users);
        organizationRepository.saveAll(orgs);
        userOrgsRepository.saveAll(userOrgs);
        return CoreCommon.UsersResp.newBuilder().addAllData(
                users.stream().map(User::toProto).collect(Collectors.toList())
        );
    }

    public CoreCommon.UserWithOrgAuth.Builder readUserAuth(DalUser.ReadUserReq request) {
        CoreCommon.UserWithOrgAuth.Builder resp = CoreCommon.UserWithOrgAuth.newBuilder();
        com.bootapp.core.domain.User user = new com.bootapp.core.domain.User();
        CoreCommon.User req = request.getUser();
        QUser qUser = QUser.user;

        BooleanExpression queryExpressions = null;
        user.fromProto(req);
        if (user.getId() != 0L)
            queryExpressions = qUser.id.eq(user.getId());
        else {
            if (user.getUsername() != null && !user.getUsername().equals(""))
                queryExpressions = qUser.username.eq(user.getUsername()).or(queryExpressions);
            if (user.getEmail() != null && !user.getEmail().equals(""))
                queryExpressions = qUser.email.eq(user.getEmail()).or(queryExpressions);
            if (user.getPhone() != null && !user.getPhone().equals(""))
                queryExpressions = qUser.phone.eq(user.getPhone()).or(queryExpressions);
        }
        if (queryExpressions == null) throw GrpcStatusException.GrpcNotFoundException();
        User dbUser = queryFactory.selectFrom(qUser).where(queryExpressions).fetchOne();
        if (dbUser == null) throw GrpcStatusException.GrpcNotFoundException();

        if (dbUser.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE)
            throw GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:inactivated");
        if (req.getId() == 0 && req.hasPassword() && !BCrypt.checkpw(req.getPassword().getValue(), dbUser.getPasswordHash()))
            throw GrpcStatusException.GrpcInvalidArgException("INVALID_PASSWORD");
        resp.setUser(dbUser.toProto());
        List<UserOrgs> userOrgsList;
        if (request.getOrgId() != 0) {
            Optional<UserOrgs> userOrgs = userOrgsRepository.findOneByUserIdAndOrgId(dbUser.getId(), request.getOrgId());
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

    public CoreCommon.UsersResp.Builder readUsers(DalUser.ReadUsersReq request) {
        CoreCommon.UsersResp.Builder resp = CoreCommon.UsersResp.newBuilder();
        CoreCommon.User req = request.getUser();
        QUser qUser = QUser.user;
        QUserOrgs qUserOrgs = QUserOrgs.userOrgs;

        BooleanExpression expr = null;

        if (req.hasUsername()) expr = qUser.username.like(req.getUsername().getValue() + "%");
        if (req.hasEmail()) expr = qUser.email.like(req.getEmail().getValue() + "%").or(expr);
        if (req.hasPhone()) expr = qUser.phone.like(req.getPhone().getValue() + "%").or(expr);
        if (req.getOrgId() != 0L) expr =  qUserOrgs.orgId.eq(req.getOrgId()).and(expr);

        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;

        QueryResults<User> qRes;
        if (expr != null) {
            qRes = queryFactory.select(qUser).from(qUser)
                    .innerJoin(qUserOrgs).on(qUserOrgs.userId.eq(qUser.id))
                    .where(expr).offset(request.getPagination().getIdx()).limit(size).fetchResults();
        } else
            qRes = queryFactory.select(qUser).from(qUser)
                    .offset(request.getPagination().getIdx()).limit(size).fetchResults();

        qRes.getResults().forEach(it -> resp.addData(it.toProto()));
        resp.setPagination(CoreCommon.Pagination.newBuilder()
                .setTotal(qRes.getTotal()).setIdx(qRes.getOffset()).setSize(size));
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

    public CoreCommon.UsersResp.Builder readUsersIn(DalUser.ReadUsersInReq request) {
        QUser qUser = QUser.user;

        BooleanExpression exp = null;
        if (request.getUserIdsCount() > 0) exp = qUser.id.in(request.getUserIdsList()).and(exp);
        if (request.getPhonesCount() > 0) exp = qUser.phone.in(request.getPhonesList()).and(exp);
        if (request.getEmailsCount() > 0) exp = qUser.email.in(request.getEmailsList()).and(exp);
        if (request.getUsernamesCount() > 0) exp = qUser.username.in(request.getUsernamesList()).and(exp);
        if (request.getNamesCount() > 0) exp = qUser.name.in(request.getNamesList()).and(exp);

        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
        QueryResults<User> res =queryFactory.selectFrom(qUser)
                .where(exp).offset(request.getPagination().getIdx()).limit(size).fetchResults();

        CoreCommon.UsersResp.Builder resp = CoreCommon.UsersResp.newBuilder();
        resp.addAllData(res.getResults().stream().map(User::toProto).collect(Collectors.toList()));

        resp.setPagination(CommonUtils.buildPagination(res.getOffset(), res.getLimit(), res.getTotal()));
        return resp;
    }
    public void verifyUniqueUser(CoreCommon.User request) {
        if (request.hasUsername()) {
            if (userRepository.findOneByUsername(request.getUsername().getValue()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:username");
        }
        if (request.hasPhone()) {
            if (userRepository.findOneByPhone(request.getPhone().getValue()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:phone");
        }
        if (request.hasEmail()) {
            if (userRepository.findOneByEmail(request.getEmail().getValue()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:email");
        }
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
    public void updateMessages(DalUser.MessagesReq request) {
        Map<Long, CoreCommon.Message> msgMap = new HashMap<>();
        List<Message> messagesToSave = new ArrayList<>();
        List<Inbox> inboxToSave = new ArrayList<>();
        request.getDataList().forEach(x -> {
            if (x.getTo() == 0L) return;
            if (x.getId() == 0L) {
                Message message = new Message();
                message.setId(idGen.nextId());
                message.fromProto(x);
                if (message.getUserId() == 0L) message.setUserId(request.getUserId());
                if (message.getOrgId() == 0L) message.setOrgId(request.getOrgId());
                messagesToSave.add(message);
                Inbox inbox = new Inbox();
                inbox.setId(idGen.nextId());
                inbox.setUserId(x.getTo());
                inbox.setMsgId(message.getId());
                inbox.setStatus(CoreCommon.EntityStatus.ENTITY_STATUS_INACTIVATED_VALUE);
                inboxToSave.add(inbox);
            } else {
                msgMap.put(x.getId(), x);
            }
        });
        if (msgMap.size() > 0) {
            List<Message> msgs = messageRepository.findAllById(msgMap.keySet());
            msgs.forEach(x -> {
                if (!msgMap.containsKey(x.getId())) return;
                x.fromProto(msgMap.get(x.getId()));
                messagesToSave.add(x);
            });
        }
        messageRepository.saveAll(messagesToSave);
        inboxRepository.saveAll(inboxToSave);
    }

    public CoreCommon.MessageResp.Builder readMessages(DalUser.ReadMessagesReq request) {
        CoreCommon.MessageResp.Builder builder = CoreCommon.MessageResp.newBuilder();
        QMessage qMsg = QMessage.message;
        QInbox qIn = QInbox.inbox;
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
        BooleanExpression qe;
        // ===== msg type
        if (request.getType() == CoreCommon.MessageType.MESSAGE_TYPE_NULL)
            qe = qMsg.type.eq(CoreCommon.MessageType.MESSAGE_TYPE_USER_VALUE);
        else
            qe = qMsg.type.eq(request.getTypeValue());
        // ===== receive type
        if (request.getReceiveType() == CoreCommon.ReceiveType.RECEIVE_TYPE_NULL)
            qe = qMsg.receiveType.eq(CoreCommon.ReceiveType.RECEIVE_TYPE_SITE_VALUE).and(qe);
        else
            qe = qMsg.receiveType.eq(request.getReceiveTypeValue()).and(qe);

        if (!request.getTitle().equals(""))
            qe = qMsg.title.startsWith(request.getTitle()).and(qe);
        if (!request.getContent().equals(""))
            qe = qMsg.content.startsWith(request.getContent()).and(qe);

        if (request.getUserId() == request.getToUserId()) {
            qe = qIn.userId.eq(request.getToUserId()).and(qe);
            if (request.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
                qe = qIn.status.eq(request.getStatusValue()).and(qe);
            QueryResults<Tuple> results = queryFactory.select(qIn, qMsg).from(qIn).leftJoin(qMsg).on(qMsg.id.eq(qIn.msgId))
                    .where(qe).offset(request.getPagination().getIdx()).limit(size).fetchResults();
            results.getResults().forEach(x -> {
                Message msg = x.get(qMsg);
                Inbox inbox = x.get(qIn);
                if (msg == null || inbox == null) return;
                CoreCommon.Message.Builder msgBuilder = msg.toProtoBuilder();
                msgBuilder.setStatusValue(inbox.getStatus());
                builder.addData(msgBuilder.build());
            });
            builder.setPagination(CommonUtils.buildPagination(results.getOffset(), results.getOffset(), results.getTotal()));
        } else {
            qe = qMsg.userId.eq(request.getFromUserId())
                    .and(qMsg.status.eq(CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE)).and(qe);
            if (request.getToUserId() != 0L) qe = qMsg.sendTo.eq(request.getToUserId()).and(qe);
            QueryResults<Message> results = queryFactory.selectFrom(qMsg)
                    .where(qe).offset(request.getPagination().getIdx()).limit(size).fetchResults();
            builder.addAllData(results.getResults().stream().map(Message::toProto).collect(Collectors.toList()));
            builder.setPagination(CommonUtils.buildPagination(results.getOffset(), results.getOffset(), results.getTotal()));
        }
        return builder;
    }

    @Transactional
    public void updateInbox(DalUser.UpdateInboxReq request) {
        List<Inbox> items = inboxRepository.findAllById(request.getIdsList());
        items.forEach(x -> x.setStatus(request.getStatusValue()));
        inboxRepository.saveAll(items);
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
        if (!request.getContactName().equals("")) query = qUser.name.like(request.getContactName() + "%").or(query);
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
    public void createSimpleRelation(DalUser.SimpleRelationReq request) {
        switch (request.getType()) {
            case RELATION_SIMPLE_USER_VISIT: {
                RelationVisit item = new RelationVisit();
                item.setId(idGen.nextId());
                item.setUserId(request.getQueryUserId());
                item.setToUserId(request.getToId());
                relationVisitRepository.save(item);
            }
            break;
            case RELATION_SIMPLE_USER_FOLLOW: {
                RelationFollow item = new RelationFollow();
                item.setId(idGen.nextId());
                item.setUserId(request.getQueryUserId());
                item.setToUserId(request.getToId());
                relationFollowRepository.save(item);
            }
            break;
            case RELATION_SIMPLE_USER_BLACKLIST: {
                RelationBlacklist item = new RelationBlacklist();
                item.setId(idGen.nextId());
                item.setUserId(request.getQueryUserId());
                item.setBlockedUserId(request.getToId());
                relationBlacklistRepository.save(item);
            }
            break;
        }
    }

    @Transactional
    public void deleteSimpleRelation(DalUser.SimpleRelationReq request) {
        switch (request.getType()) {
            case RELATION_SIMPLE_USER_VISIT: {
                relationVisitRepository.deleteByUserIdAndToUserId(request.getQueryUserId(), request.getToId());
            }
            break;
            case RELATION_SIMPLE_USER_FOLLOW: {
                relationFollowRepository.deleteByUserIdAndToUserId(request.getQueryUserId(), request.getToId());
            }
            break;
            case RELATION_SIMPLE_USER_BLACKLIST: {
                relationBlacklistRepository.deleteByUserIdAndBlockedUserId(request.getQueryUserId(), request.getToId());
            }
            break;
        }
    }

    public CoreCommon.SimpleRelationList readSimpleRelations(DalUser.SimpleRelationReq request) {
        CoreCommon.SimpleRelationList.Builder builder = CoreCommon.SimpleRelationList.newBuilder();
        long size = request.getPagination().getSize();
        if (size <= 0) size = Constants.DEFAULT_PAGINATION_SIZE;
        switch (request.getType()) {
            case RELATION_SIMPLE_USER_VISIT: {
                QRelationVisit qRe = QRelationVisit.relationVisit;
                BooleanExpression be = null;
                if (request.getUserId() != 0L) be = qRe.userId.eq(request.getUserId());
                if (request.getToId() != 0L) be = qRe.toUserId.eq(request.getToId()).and(be);
                QueryResults<RelationVisit> qRes = queryFactory.selectFrom(qRe).where(be)
                        .offset(request.getPagination().getIdx()).limit(size)
                        .orderBy(qRe.updatedAt.desc()).fetchResults();
                builder.addAllData(qRes.getResults().stream().map(RelationVisit::toProto).collect(Collectors.toList()));
                builder.setPagination(CommonUtils.buildPagination(qRes.getOffset(), qRes.getLimit(), qRes.getTotal()));
            }
            break;
            case RELATION_SIMPLE_USER_FOLLOW: {
                QRelationFollow qRe = QRelationFollow.relationFollow;
                BooleanExpression be = null;
                if (request.getUserId() != 0L) be = qRe.userId.eq(request.getUserId());
                if (request.getToId() != 0L) be = qRe.toUserId.eq(request.getToId()).and(be);
                QueryResults<RelationFollow> qRes = queryFactory.selectFrom(qRe).where(be)
                        .offset(request.getPagination().getIdx()).limit(size)
                        .orderBy(qRe.updatedAt.desc()).fetchResults();
                builder.addAllData(qRes.getResults().stream().map(RelationFollow::toProto).collect(Collectors.toList()));
                builder.setPagination(CommonUtils.buildPagination(qRes.getOffset(), qRes.getLimit(), qRes.getTotal()));
            }
            break;
            case RELATION_SIMPLE_USER_BLACKLIST: {
                QRelationBlacklist qRe = QRelationBlacklist.relationBlacklist;
                BooleanExpression be = null;
                if (request.getUserId() != 0L) be = qRe.userId.eq(request.getUserId());
                if (request.getToId() != 0L) be = qRe.blockedUserId.eq(request.getToId()).and(be);
                QueryResults<RelationBlacklist> qRes = queryFactory.selectFrom(qRe).where(be)
                        .offset(request.getPagination().getIdx()).limit(size)
                        .orderBy(qRe.updatedAt.desc()).fetchResults();
                builder.addAllData(qRes.getResults().stream().map(RelationBlacklist::toProto).collect(Collectors.toList()));
                builder.setPagination(CommonUtils.buildPagination(qRes.getOffset(), qRes.getLimit(), qRes.getTotal()));
            }
            break;
        }
        return builder.build();
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
        } else if (request.getPid() != 0) {
            QueryResults<DictItem> qR = queryFactory.selectFrom(qDict)
                    .where(qDict.pid.eq(request.getPid()))
                    .offset(request.getPagination().getIdx()).limit(size).fetchResults();
            dictItems = qR.getResults();
            builder.setPagination(CommonUtils.buildPagination(qR.getOffset(), qR.getLimit(), qR.getTotal()));
        } else {
            QueryResults<DictItem> qR = queryFactory.selectFrom(qDict)
                    .where(qDict.pid.eq(request.getPid()))
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
}
