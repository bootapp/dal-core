package com.bootapp.core.service;

import com.bootapp.core.domain.*;
import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalUser;
import com.bootapp.core.grpc.DalUserServiceGrpc;
import com.bootapp.core.repository.*;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import com.bootapp.core.utils.idgen.IDGenerator;
import com.querydsl.core.types.dsl.BooleanExpression;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@GRpcService
public class UserService extends DalUserServiceGrpc.DalUserServiceImplBase {
    private final UserRepository userRepository;
    private final UserOrgsRepository userOrgsRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleOrgRepository roleOrgRepository;
    private final RoleUserRepository roleUserRepository;
    private final IDGenerator idGen;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String usernameRE = "[+\\-@#$%^&*()!~`|?<>;'\"]+";
    public UserService(UserRepository userRepository, UserOrgsRepository userOrgsRepository, OrganizationRepository organizationRepository, RoleOrgRepository roleOrgRepository, RoleUserRepository roleUserRepository, IDGenerator idGen) {
        this.userRepository = userRepository;
        this.userOrgsRepository = userOrgsRepository;
        this.organizationRepository = organizationRepository;
        this.roleOrgRepository = roleOrgRepository;
        this.roleUserRepository = roleUserRepository;
        this.idGen = idGen;
    }
    @Override
    @Transactional
    public void writeUser(CoreCommon.User request, StreamObserver<CoreCommon.UserWithOrg> responseObserver) {
        CoreCommon.UserWithOrg.Builder resp = CoreCommon.UserWithOrg.newBuilder();
        try {
            User user = new User();
            user.fromProto(request);
            //------------ handle userId
            if (request.getId() != 0L) user.setId(request.getId());
            else user.setId(idGen.nextId());
            if (user.getId() == 0L) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("the new user's id is zero"));
                return;
            }

            if (!request.getPassword().equals("")) user.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

            if (user.getUsername() != null && user.getUsername().matches(usernameRE)) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("wrong username"));
                return;
            }
            userRepository.save(user);
            // add to default user group
            UserOrgs userOrgs = new UserOrgs(idGen.nextId(), user.getId(), 1, 1);
            userOrgsRepository.save(userOrgs);
            logger.info("new user saved to db with id: {}", user.getId());
            resp.setUser(user.toProto());
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
        } catch (DataIntegrityViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException(new RuntimeException(e.getMostSpecificCause().getMessage())));
        } catch (ConstraintViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcInvalidArgException(new RuntimeException(e.getConstraintName() + " wrong")));
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void checkPhoneExists(DalUser.UserPhoneReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (userRepository.findOneByPhone(request.getPhone()).isPresent()) {
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
    public void readUser(CoreCommon.User request, StreamObserver<CoreCommon.UserWithOrg> responseObserver) {
        CoreCommon.UserWithOrg.Builder resp = CoreCommon.UserWithOrg.newBuilder();
        try {
            com.bootapp.core.domain.User user = new com.bootapp.core.domain.User();
            QUser userDsl = QUser.user;
            BooleanExpression queryExpressions = null;
            user.fromProto(request);
            if (user.getId() != 0L) {
                queryExpressions = userDsl.id.eq(user.getId());
            } else {
                if(user.getUsername()!= null && !user.getUsername().equals("")) queryExpressions = userDsl.username.eq(user.getUsername()).or(queryExpressions);
                if(user.getEmail()!= null && !user.getEmail().equals("")) queryExpressions = userDsl.email.eq(user.getEmail()).or(queryExpressions);
                if(user.getPhone()!= null && !user.getPhone().equals("")) queryExpressions = userDsl.phone.eq(user.getPhone()).or(queryExpressions);
            }
            if(queryExpressions == null) {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
                return;
            }
            com.bootapp.core.domain.User dbUser = userRepository.findOne(queryExpressions).orElse(null);
            if (dbUser == null) {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
                return;
            }
            if (request.getId() == 0 && request.getPassword() != null && !BCrypt.checkpw(request.getPassword(), dbUser.getPasswordHash())) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("invalid password"));
                return;
            }
            resp.setUser(dbUser.toProto());
            List<UserOrgs> userOrgsList;
            if (request.getOrgId() != 0) {
                Optional<UserOrgs> userOrgs = userOrgsRepository.findOneByUserIdAndOrgId(dbUser.getId(), request.getOrgId());
                if(!userOrgs.isPresent()) {
                    responseObserver.onError(GrpcStatusException.GrpcInternalException("user org error"));
                    return;
                }
                userOrgsList = Collections.singletonList(userOrgs.get());
            } else {
                userOrgsList = userOrgsRepository.findAllByUserId(dbUser.getId());
            }
            // return error if user has no organization,
            // remove the default org if the user has more than one organization.
            if(userOrgsList.size() == 0) {
                responseObserver.onError(GrpcStatusException.GrpcInternalException("no org for the user"));
                return;
            } else if (userOrgsList.size() > 1) {
                for(int i = 0; i < userOrgsList.size(); i++) {
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
    public void readUsers(DalUser.ReadUsersReq request, StreamObserver<DalUser.ReadUsersResp> responseObserver) {
        DalUser.ReadUsersResp.Builder resp = DalUser.ReadUsersResp.newBuilder();
        try {
            com.bootapp.core.domain.User user = new com.bootapp.core.domain.User();
            QUser userDsl = QUser.user;
            BooleanExpression queryExpressions = null;
            user.fromProto(request.getUser());
            if(user.getUsername()!= null && !user.getUsername().equals("")) queryExpressions = userDsl.username.like(user.getUsername() + "%").or(queryExpressions);
            if(user.getEmail()!= null && !user.getEmail().equals("")) queryExpressions = userDsl.email.like(user.getEmail() + "%").or(queryExpressions);
            if(user.getPhone()!= null && !user.getPhone().equals("")) queryExpressions = userDsl.phone.like(user.getPhone() + "%").or(queryExpressions);
            int limit = request.getLimit();
            if (limit <= 0) limit = 20;
            Page<com.bootapp.core.domain.User> users = userRepository.findAll(userDsl.id.gt(request.getOffsetId()).and(queryExpressions), PageRequest.of(0, limit));

            users.forEach(it -> resp.addUsers(it.toProto()));
            responseObserver.onNext(resp.buildPartial());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
}
