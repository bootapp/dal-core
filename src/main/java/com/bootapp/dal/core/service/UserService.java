package com.bootapp.dal.core.service;

import com.bootapp.dal.core.grpc.DalCoreUserServiceGrpc;
import com.bootapp.dal.core.grpc.User;
import com.bootapp.dal.core.repository.UserRepository;
import com.bootapp.dal.core.utils.grpc.GrpcStatusException;
import com.bootapp.dal.core.utils.idgen.IDGenerator;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.grpc.stub.StreamObserver;
import org.hibernate.exception.ConstraintViolationException;
import org.lognet.springboot.grpc.GRpcService;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.bootapp.dal.core.domain.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@GRpcService
public class UserService extends DalCoreUserServiceGrpc.DalCoreUserServiceImplBase {
    private final UserRepository userRepository;
    private final IDGenerator idGen;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String usernameRE = "[+-@]+";
    public UserService(UserRepository userRepository, IDGenerator idGen) {
        this.userRepository = userRepository;
        this.idGen = idGen;
    }
    @Override
    public void invokeNewUser(User.UserInfo request, StreamObserver<User.UserQueryResp> responseObserver) {
        User.UserQueryResp.Builder resp = User.UserQueryResp.newBuilder();
        try {
            com.bootapp.dal.core.domain.User user = new com.bootapp.dal.core.domain.User();
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
    public void checkPhoneExists(User.UserPhoneReq request, StreamObserver<User.UserResp> responseObserver) {
        try {
            if (userRepository.findOneByPhone(request.getPhone()).isPresent()) {
                responseObserver.onNext(User.UserResp.newBuilder().build());
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
    public void queryUser(User.UserInfo request, StreamObserver<User.UserQueryResp> responseObserver) {
        User.UserQueryResp.Builder resp = User.UserQueryResp.newBuilder();
        try {
            com.bootapp.dal.core.domain.User user = new com.bootapp.dal.core.domain.User();
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
            com.bootapp.dal.core.domain.User dbUser = userRepository.findOne(queryExpressions).orElse(null);
            if (dbUser == null) {
                responseObserver.onError(GrpcStatusException.GrpcNotFoundException());
                return;
            }
            if (request.getPassword() != null && !BCrypt.checkpw(request.getPassword(), dbUser.getPasswordHash())) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("invalid password"));
                return;
            }
            resp.setUser(dbUser.toProto());
            responseObserver.onNext(resp.buildPartial());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }

    @Override
    public void queryUsers(User.UsersQueryReq request, StreamObserver<User.UsersQueryResp> responseObserver) {
        User.UsersQueryResp.Builder resp = User.UsersQueryResp.newBuilder();
        try {
            com.bootapp.dal.core.domain.User user = new com.bootapp.dal.core.domain.User();
            QUser userDsl = QUser.user;
            BooleanExpression queryExpressions = null;
            user.fromProto(request.getUser());
            if(user.getUsername()!= null && !user.getUsername().equals("")) queryExpressions = userDsl.username.like(user.getUsername() + "%").or(queryExpressions);
            if(user.getEmail()!= null && !user.getEmail().equals("")) queryExpressions = userDsl.email.like(user.getEmail() + "%").or(queryExpressions);
            if(user.getPhone()!= null && !user.getPhone().equals("")) queryExpressions = userDsl.phone.like(user.getPhone() + "%").or(queryExpressions);
            int limit = request.getLimit();
            if (limit <= 0) limit = 20;
            Page<com.bootapp.dal.core.domain.User> users = userRepository.findAll(userDsl.id.gt(request.getOffsetId()).and(queryExpressions), PageRequest.of(0, limit));

            users.forEach(it -> resp.addUsers(it.toProto()));
            responseObserver.onNext(resp.buildPartial());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
}
