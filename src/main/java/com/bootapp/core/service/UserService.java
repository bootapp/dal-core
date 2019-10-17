package com.bootapp.core.service;

import com.bootapp.core.domain.*;
import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalUser;
import com.bootapp.core.grpc.DalUserServiceGrpc;
import com.bootapp.core.repository.*;
import com.bootapp.core.utils.CommonUtils;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import com.bootapp.core.utils.idgen.IDGenerator;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.grpc.stub.StreamObserver;
import org.hibernate.exception.ConstraintViolationException;
import org.lognet.springboot.grpc.GRpcService;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@GRpcService
public class UserService extends DalUserServiceGrpc.DalUserServiceImplBase {
    private final UserRepository userRepository;
    private final UserOrgsRepository userOrgsRepository;
    private final RelationsRepository relationsRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final MessageRepository messageRepository;
    private final RoleOrgRepository roleOrgRepository;
    private final RoleUserRepository roleUserRepository;
    private final IDGenerator idGen;
    private final EntityManager em;
    private JPAQueryFactory queryFactory;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String usernameRE = "[+\\-@#$%^&*()!~`|?<>;'\"]+";
    public UserService(UserRepository userRepository, UserOrgsRepository userOrgsRepository, RelationsRepository relationsRepository, OrganizationRepository organizationRepository, DepartmentRepository departmentRepository, MessageRepository messageRepository, RoleOrgRepository roleOrgRepository, RoleUserRepository roleUserRepository, IDGenerator idGen, EntityManager em) {
        this.userRepository = userRepository;
        this.userOrgsRepository = userOrgsRepository;
        this.relationsRepository = relationsRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.messageRepository = messageRepository;
        this.roleOrgRepository = roleOrgRepository;
        this.roleUserRepository = roleUserRepository;
        this.idGen = idGen;
        this.em = em;
        queryFactory = new JPAQueryFactory(this.em);
    }
    @Override
    @Transactional
    public void createUser(DalUser.CreateUserReq request, StreamObserver<CoreCommon.UserWithOrgAuth> responseObserver) {
        CoreCommon.UserWithOrgAuth.Builder resp = CoreCommon.UserWithOrgAuth.newBuilder();
        try {
            CoreCommon.User req = request.getUser();
            User user = new User();
            user.fromProto(req);
            //------------ check username
            if (user.getUsername() != null && user.getUsername().matches(usernameRE)) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:username"));
                return;
            }
            //------------ set password
            if (req.hasPassword())
                user.setPasswordHash(BCrypt.hashpw(req.getPassword().getValue(), BCrypt.gensalt()));
            user.setId(idGen.nextId());
            UserOrgs userOrgs = new UserOrgs(idGen.nextId(), user.getId(), 1, 1);
            userOrgsRepository.save(userOrgs);
            logger.info("new user saving to db with id: {}", user.getId());
            userRepository.save(user);
            resp.setUser(user.toProto());
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
        } catch (DataIntegrityViolationException e) {
                responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException(new RuntimeException("ALREADY_EXISTS:"+e.getMostSpecificCause().getMessage())));
        } catch (ConstraintViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcInvalidArgException(new RuntimeException("INVALID_ARGS:"+e.getConstraintName())));
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void createUsers(DalUser.CreateUsersReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        try {
            List<User> users = request.getDataList().stream().map(x -> {
                User newUser = new User();
                newUser.fromProto(x);
                newUser.setId(idGen.nextId());
                return newUser;
            }).collect(Collectors.toList());
            userRepository.saveAll(users);
            responseObserver.onNext(CoreCommon.UsersResp.newBuilder().addAllData(
                    users.stream().map(User::toProto).collect(Collectors.toList())
            ).build());
            responseObserver.onCompleted();
        }catch (DataIntegrityViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException(new RuntimeException("ALREADY_EXISTS:"+e.getMostSpecificCause().getMessage())));
        } catch (ConstraintViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcInvalidArgException(new RuntimeException("INVALID_ARGS:"+e.getConstraintName())));
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readUserAuth(DalUser.ReadUserReq request, StreamObserver<CoreCommon.UserWithOrgAuth> responseObserver) {
        CoreCommon.UserWithOrgAuth.Builder resp = CoreCommon.UserWithOrgAuth.newBuilder();
        try {
            com.bootapp.core.domain.User user = new com.bootapp.core.domain.User();
            CoreCommon.User req = request.getUser();
            QUser userDsl = QUser.user;
            BooleanExpression queryExpressions = null;
            user.fromProto(req);
            if (user.getId() != 0L) {
                queryExpressions = userDsl.id.eq(user.getId());
            } else {
                if (user.getUsername() != null && !user.getUsername().equals(""))
                    queryExpressions = userDsl.username.eq(user.getUsername()).or(queryExpressions);
                if (user.getEmail() != null && !user.getEmail().equals(""))
                    queryExpressions = userDsl.email.eq(user.getEmail()).or(queryExpressions);
                if (user.getPhone() != null && !user.getPhone().equals(""))
                    queryExpressions = userDsl.phone.eq(user.getPhone()).or(queryExpressions);
            }
            if (queryExpressions == null) {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
                return;
            }
            com.bootapp.core.domain.User dbUser = userRepository.findOne(queryExpressions).orElse(null);
            if (dbUser == null) {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
                return;
            }
            if (dbUser.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NORMAL_VALUE) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:inactivated"));
                return;
            }
            if (req.getId() == 0 && req.hasPassword() && !BCrypt.checkpw(req.getPassword().getValue(), dbUser.getPasswordHash())) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_PASSWORD"));
                return;
            }
            resp.setUser(dbUser.toProto());
            List<UserOrgs> userOrgsList;
            if (request.getOrgId() != 0) {
                Optional<UserOrgs> userOrgs = userOrgsRepository.findOneByUserIdAndOrgId(dbUser.getId(), request.getOrgId());
                if (!userOrgs.isPresent()) {
                    responseObserver.onError(GrpcStatusException.GrpcInternalException("NON_EXISTS"));
                    return;
                }
                userOrgsList = Collections.singletonList(userOrgs.get());
            } else {
                userOrgsList = userOrgsRepository.findAllByUserId(dbUser.getId());
            }
            // return error if user has no organization,
            // remove the default org if the user has more than one organization.
            if (userOrgsList.size() == 0) {
                responseObserver.onError(GrpcStatusException.GrpcInternalException("NON_EXISTS"));
                return;
            } else if (userOrgsList.size() > 1) {
                for (int i = 0; i < userOrgsList.size(); i++) {
                    if (userOrgsList.get(i).getOrgId() == 1) {
                        userOrgsList.remove(i);
                        break;
                    }
                }
            }
            userOrgsList.forEach(x -> {
                CoreCommon.OrgWithAuthorities.Builder userOrgBuilder = CoreCommon.OrgWithAuthorities.newBuilder();
                Organization organization = organizationRepository.findById(x.getOrgId()).get();
                RoleOrg roleOrg = roleOrgRepository.findById(organization.getOrgRoleId()).get();
                RoleUser roleUser = roleUserRepository.findById(x.getUserRoleId()).get();
                userOrgBuilder.setId(x.getOrgId());
                userOrgBuilder.setName(organization.getName());
                userOrgBuilder.setAuthorities(roleUser.getAuthorities());
                userOrgBuilder.setAuthorityGroups(roleOrg.getAuthorities());
                resp.addOrgInfo(userOrgBuilder.buildPartial());
            });
            responseObserver.onNext(resp.buildPartial());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateUser(DalUser.UpdateUserReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getType() != DalUser.UpdateUserType.UPDATE_USER_TYPE_ID && request.getUser().getId() != 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARGS:id can be non-zero only for updateById"));
                return;
            }
            Optional<User> user;
            switch (request.getType()) {
                case UPDATE_USER_TYPE_ID:
                    if (request.getUser().getId() == 0) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:userId"));
                        return;
                    }
                    user = userRepository.findById(request.getUser().getId());
                    break;
                case UPDATE_USER_TYPE_USERNAME:
                    if (!request.getUser().hasUsername()) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:username"));
                        return;
                    }
                    user = userRepository.findOneByUsername(request.getUser().getUsername().getValue());
                    break;
                case UPDATE_USER_TYPE_PHONE:
                    if (!request.getUser().hasPhone()) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:phone"));
                        return;
                    }
                    user = userRepository.findOneByPhone(request.getUser().getPhone().getValue());
                    break;
                case UPDATE_USER_TYPE_EMAIL:
                    if (!request.getUser().hasEmail()) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:email"));
                        return;
                    }
                    user = userRepository.findOneByEmail(request.getUser().getEmail().getValue());
                    break;
                default:
                    responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:type"));
                    return;
            }
            if (!user.isPresent()) {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
                return;
            }
            User dbUser = user.get();
            dbUser.fromProto(request.getUser());
            //------------ set password
            if (request.getUser().hasPassword())
                dbUser.setPasswordHash(BCrypt.hashpw(request.getUser().getPassword().getValue(), BCrypt.gensalt()));
            userRepository.save(dbUser);

            responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }
    @Override
    public void readUsers(DalUser.ReadUsersReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        CoreCommon.UsersResp.Builder resp = CoreCommon.UsersResp.newBuilder();
        try {
            CoreCommon.User req = request.getUser();
            QUser qUser = QUser.user;
            QUserOrgs qUserOrgs = QUserOrgs.userOrgs;

            BooleanExpression expr = null;

            if (req.hasUsername()) expr = qUser.username.like(req.getUsername().getValue() + "%");
            if (req.hasEmail()) expr = qUser.email.like(req.getEmail().getValue() + "%").or(expr);
            if (req.hasPhone()) expr = qUser.phone.like(req.getPhone().getValue() + "%").or(expr);
            if (req.getOrgId() != 0L) expr =  qUserOrgs.orgId.eq(req.getOrgId()).and(expr);

            long page = request.getPagination().getIdx();
            long size = request.getPagination().getSize();
            if (size <= 0) size = 20;

            QueryResults<User> qRes;
            if (expr != null) {
                qRes = queryFactory.select(qUser).from(qUser)
                        .innerJoin(qUserOrgs).on(qUserOrgs.userId.eq(qUser.id))
                        .where(expr).offset(page).limit(size).fetchResults();
            } else
                qRes = queryFactory.select(qUser).from(qUser).offset(page).limit(size).fetchResults();

            qRes.getResults().forEach(it -> resp.addData(it.toProto()));
            resp.setPagination(CoreCommon.Pagination.newBuilder()
                    .setTotal(qRes.getTotal()).setIdx(qRes.getOffset()).setSize(size));

            responseObserver.onNext(resp.buildPartial());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }

    @Override
    public void readUsersIn(DalUser.ReadUsersInReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        try {
            QUser qUser = QUser.user;

            BooleanExpression exp = null;
            if (request.getUserIdsCount() > 0) exp = qUser.id.in(request.getUserIdsList()).and(exp);
            if (request.getPhonesCount() > 0) exp = qUser.phone.in(request.getPhonesList()).and(exp);
            if (request.getEmailsCount() > 0) exp = qUser.email.in(request.getEmailsList()).and(exp);
            if (request.getUsernamesCount() > 0) exp = qUser.username.in(request.getUsernamesList()).and(exp);
            if (request.getNamesCount() > 0) exp = qUser.name.in(request.getNamesList()).and(exp);

            long size = request.getPagination().getSize();
            if (size <= 0) size = 20;
            QueryResults<User> res =queryFactory.selectFrom(qUser)
                    .where(exp).offset(request.getPagination().getIdx()).limit(size).fetchResults();

            CoreCommon.UsersResp.Builder resp = CoreCommon.UsersResp.newBuilder();
            resp.addAllData(res.getResults().stream().map(User::toProto).collect(Collectors.toList()));

            resp.setPagination(CommonUtils.buildPagination(res.getOffset(), res.getLimit(), res.getTotal()));
            responseObserver.onNext(resp.buildPartial());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }

    @Override
    public void verifyUniqueUser(CoreCommon.User request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.hasUsername()) {
                if (userRepository.findOneByUsername(request.getUsername().getValue()).isPresent()) {
                    responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:username"));
                    return;
                }
            }
            if (request.hasPhone()) {
                if (userRepository.findOneByPhone(request.getPhone().getValue()).isPresent()) {
                    responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:phone"));
                    return;
                }
            }
            if (request.hasEmail()) {
                if (userRepository.findOneByEmail(request.getEmail().getValue()).isPresent()) {
                    responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:email"));
                    return;
                }
            }
            responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void createMessage(DalUser.UpdateMessageReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            Message msg = new Message();
            msg.setId(idGen.nextId());
            msg.fromProto(request.getMessage());
            if (msg.getUserId() == 0L) msg.setUserId(request.getUserId());
            if (msg.getOrgId() == 0L) msg.setOrgId(request.getOrgId());
            responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readMessages(DalUser.ReadMessagesReq request, StreamObserver<CoreCommon.MessageResp> responseObserver) {
        try {
            CoreCommon.Message msgReq = request.getMessage();
            QMessage qMessage = QMessage.message;

            BooleanExpression queryExpressions = qMessage.userId.eq(request.getUserId());
            if (msgReq.getType() != CoreCommon.MessageType.MESSAGE_TYPE_NULL)
                queryExpressions = queryExpressions.and(qMessage.type.eq(msgReq.getTypeValue()));
            if (msgReq.getReceiveType() != CoreCommon.ReceiveType.RECEIVE_TYPE_NULL)
                queryExpressions = queryExpressions.and(qMessage.receiveType.eq(msgReq.getReceiveTypeValue()));
            if (msgReq.getTo() != 0L)
                queryExpressions = queryExpressions.and(qMessage.sendTo.eq(msgReq.getTo()));
            if (msgReq.getUpdateAt()  != 0L)
                queryExpressions = queryExpressions.and(qMessage.updateAt.lt(msgReq.getUpdateAt()));
            if (msgReq.hasTitle())
                queryExpressions = queryExpressions.and(qMessage.title.like(msgReq.getTitle().getValue() + "%"));
            if (msgReq.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
                queryExpressions = queryExpressions.and(qMessage.status.eq(msgReq.getStatusValue()));
            long limit = request.getPagination().getSize();
            if (limit <= 0) limit = 20;
            Page<Message> msgPage = messageRepository.findAll(queryExpressions, PageRequest.of((int)request.getPagination().getIdx(), (int)limit));
            CoreCommon.MessageResp.Builder respBuilder = CoreCommon.MessageResp.newBuilder();

            CoreCommon.Pagination.Builder pagination = CoreCommon.Pagination.newBuilder();
            respBuilder.addAllData(msgPage.getContent().stream().map(Message::toProto).collect(Collectors.toList()));
            pagination.setTotal(msgPage.getTotalElements());
            pagination.setIdx(msgPage.getNumber());
            pagination.setSize(msgPage.getSize());
            respBuilder.setPagination(pagination);

            responseObserver.onNext(respBuilder.buildPartial());
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateMessage(DalUser.UpdateMessageReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            Optional<Message> msgOptional = messageRepository.findById(request.getMessage().getId());
            if (msgOptional.isPresent()) {
                Message msg = msgOptional.get();
                msg.fromProto(request.getMessage());
                messageRepository.save(msg);
                responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
            }
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readInbox(DalUser.ReadInboxReq request, StreamObserver<CoreCommon.MessageResp> responseObserver) {

        super.readInbox(request, responseObserver);
    }

    @Override
    public void updateInbox(DalUser.UpdateInboxReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        super.updateInbox(request, responseObserver);
    }

    @Override
    public void deleteInbox(DalUser.UpdateInboxReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        super.deleteInbox(request, responseObserver);
    }

    @Override
    public void createRelations(DalUser.RelationsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            List<Relation> relationList = request.getRelationList().stream().map(x -> {
                Relation relat = new Relation();
                relat.setId(idGen.nextId());
                relat.fromProto(x);
                return relat;
            }).collect(Collectors.toList());
            relationsRepository.saveAll(relationList);
            responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateRelations(DalUser.RelationsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            Map<Long, CoreCommon.Relation> relationsMap = request.getRelationList().stream()
                    .collect(Collectors.toMap(CoreCommon.Relation::getId, x -> x));

            List<Relation> relations = relationsRepository.findAllById(relationsMap.keySet());
            for(Relation relation : relations) {
                relation.fromProto(relationsMap.get(relation.getId()));
            }
            relationsRepository.saveAll(relations);
            responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Transactional
    @Override
    public void deleteRelations(CoreCommon.AuthorizedIdsQueryReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            relationsRepository.deleteByIdIn(request.getIdsList());
            responseObserver.onNext(CoreCommon.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    private QueryResults<Relation> queryRelations(CoreCommon.Relation req, long idx, long size) {
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

    @Override
    public void readRelations(DalUser.ReadRelationReq request, StreamObserver<CoreCommon.RelationsResp> responseObserver) {
        try {
            CoreCommon.RelationsResp.Builder builder = CoreCommon.RelationsResp.newBuilder();
            long size = request.getPagination().getSize();
            if (size == 0L) size = 20L;
            QueryResults<Relation> res = queryRelations(request.getRelation(), request.getPagination().getIdx(), size);
            List<Relation> relations = res.getResults();
            builder.addAllRelations(relations.stream().map(Relation::toProto).collect(Collectors.toList()));
            builder.setPagination(CommonUtils.buildPagination(res.getOffset(), res.getLimit(), res.getTotal()));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readDeptRelations(DalUser.ReadRelationReq request, StreamObserver<CoreCommon.DeptRelationsResp> responseObserver) {
        try {
            CoreCommon.DeptRelationsResp.Builder builder = CoreCommon.DeptRelationsResp.newBuilder();

            long size = request.getPagination().getSize();
            if (size == 0L) size = 20;

            QueryResults<Relation> res = queryRelations(request.getRelation(), request.getPagination().getIdx(), size);

            List<Relation> relations = res.getResults();
            builder.addAllRelations(relations.stream().map(Relation::toProto).collect(Collectors.toList()));
            Map<Long, CoreCommon.User> sourceMap = new HashMap<>();
            Map<Long, CoreCommon.Department> targetMap = new HashMap<>();
            for (Relation r : relations) {
                sourceMap.put(r.getSourceId(), null);
                targetMap.put(r.getTargetId(), null);
            }
            List<User> users = userRepository.findAllById(sourceMap.keySet());
            List<Department> depts = departmentRepository.findAllById(targetMap.keySet());
            for (User user : users) sourceMap.put(user.getId(), user.toProto());
            for (Department dept: depts) targetMap.put(dept.getId(), dept.toProto());
            builder.setPagination(CommonUtils.buildPagination(res.getOffset(), res.getLimit(), res.getTotal()));

            builder.putAllUserMap(sourceMap);
            builder.putAllDeptMap(targetMap);

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }
    @Override
    public void readPartnerRelations(DalUser.ReadRelationReq request, StreamObserver<CoreCommon.PartnerRelationsResp> responseObserver) {
        try {
            CoreCommon.PartnerRelationsResp.Builder builder = CoreCommon.PartnerRelationsResp.newBuilder();
            long size = request.getPagination().getSize();
            if (size == 0L) size = 20;
            QueryResults<Relation> res = queryRelations(request.getRelation(), request.getPagination().getIdx(), size);

            List<Relation> relations = res.getResults();
            builder.addAllRelations(relations.stream().map(Relation::toProto).collect(Collectors.toList()));

            Map<Long, CoreCommon.Organization> sourceMap = new HashMap<>();
            Map<Long, CoreCommon.User> targetMap = new HashMap<>();
            for (Relation r : relations) {
                sourceMap.put(r.getSourceId(), null);
                targetMap.put(r.getTargetId(), null);
            }

            QUser qUser = QUser.user;
            QUserOrgs qUserOrgs = QUserOrgs.userOrgs;

            List<Tuple> userOrgRes = queryFactory.select(qUser, qUserOrgs).from(qUserOrgs).leftJoin(qUser).on(qUser.id.eq(qUserOrgs.userId))
                    .where(qUserOrgs.userId.in(targetMap.keySet())).fetch();

            for(Tuple tuple: userOrgRes) {
                UserOrgs org = tuple.get(qUserOrgs);
                if (org != null && org.getOrgId() != 0L && org.getOrgId() != 1L) sourceMap.put(org.getOrgId(), null);
                User user = tuple.get(qUser);
                if (user != null) targetMap.put(user.getId(), user.toProto());
            }

            List<Organization> organizations = organizationRepository.findAllById(targetMap.keySet());
            for (Organization org : organizations) sourceMap.put(org.getId(), org.toProto());
            builder.setPagination(CommonUtils.buildPagination(res.getOffset(), res.getLimit(), res.getTotal()));

            builder.putAllOrgMap(sourceMap);
            builder.putAllUserMap(targetMap);

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readOrgPartnersIn(DalUser.ReadOrgPartnersInReq request, StreamObserver<CoreCommon.PartnerRelationsResp> responseObserver) {
        try {
            CoreCommon.PartnerRelationsResp.Builder builder = CoreCommon.PartnerRelationsResp.newBuilder();
            QRelation qRelations = QRelation.relation;
            List<Relation> relationList = queryFactory.selectFrom(qRelations).where(
                    qRelations.sourceId.eq(request.getOrgId())
                            .and(qRelations.targetId.in(request.getPartnerIdsList()))).fetch();
            builder.addAllRelations(relationList.stream().map(Relation::toProto).collect(Collectors.toList()));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }
}
