package com.bootapp.core.controller;

import com.bootapp.core.domain.*;
import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalUser;
import com.bootapp.core.grpc.DalUserServiceGrpc;
import com.bootapp.core.service.UserService;
import com.bootapp.core.utils.CommonUtils;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import com.querydsl.core.QueryResults;
import io.grpc.stub.StreamObserver;
import org.hibernate.exception.ConstraintViolationException;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.stream.Collectors;

@GRpcService
public class UserController extends DalUserServiceGrpc.DalUserServiceImplBase {

    private final UserService userService;
    private static final String usernameRE = "[a-zA-Z0-9_\\u4e00-\\u9fa5]{4,30}";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void createUser(DalUser.CreateUserReq request, StreamObserver<CoreCommon.UserWithOrgAuth> responseObserver) {

        try {
            //------------ check username
            if (request.getUser().hasUsername() && !request.getUser().getUsername().getValue().matches(usernameRE)) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:username pattern"));
                return;
            }
            responseObserver.onNext(userService.saveUser(request).build());
            responseObserver.onCompleted();
        } catch (DataIntegrityViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcAlreadyExistsException(new RuntimeException("ALREADY_EXISTS:"+e.getMostSpecificCause().getMessage())));
        } catch (ConstraintViolationException e) {
            responseObserver.onError(GrpcStatusException.GrpcInvalidArgException(new RuntimeException("INVALID_ARGS:"+e.getConstraintName())));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e.getMessage()));
        }
    }
    @Override
    public void createUsers(DalUser.CreateUsersReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        try {
            responseObserver.onNext(userService.createUsers(request).build());
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
    public void createUsersWithNewOrg(DalUser.CreateUsersWithOrgReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        try {
            responseObserver.onNext(userService.createUsersWithOrg(request).build());
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
        try {
            responseObserver.onNext(userService.readUserAuth(request).build());
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
            switch (request.getType()) {
                case UPDATE_USER_TYPE_ID:
                    if (request.getUser().getId() == 0) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:userId"));
                        return;
                    }
                    break;
                case UPDATE_USER_TYPE_USERNAME:
                    if (!request.getUser().hasUsername()) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:username"));
                        return;
                    }
                    break;
                case UPDATE_USER_TYPE_PHONE:
                    if (!request.getUser().hasPhone()) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:phone"));
                        return;
                    }
                    break;
                case UPDATE_USER_TYPE_EMAIL:
                    if (!request.getUser().hasEmail()) {
                        responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:email"));
                        return;
                    }
                    break;
                default:
                    responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:type"));
                    return;
            }
            userService.updateUser(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            responseObserver.onError(e);
        }
    }
    @Override
    public void readUsers(DalUser.ReadUsersReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        try {

            responseObserver.onNext(userService.readUsers(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }

    @Override
    public void readUsersIn(DalUser.ReadUsersInReq request, StreamObserver<CoreCommon.UsersResp> responseObserver) {
        try {

            responseObserver.onNext(userService.readUsersIn(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }

    @Override
    public void verifyUniqueUser(CoreCommon.User request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.verifyUniqueUser(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateOrgs(DalUser.OrgsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            userService.updateOrgs(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readOrgs(DalUser.ReadOrgsReq request, StreamObserver<CoreCommon.OrgsResp> responseObserver) {
        try {
            responseObserver.onNext(userService.readOrgs(request).build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateUserOrgs(DalUser.CreateUserOrgsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.updateUserOrgs(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readUserOrgs(CoreCommon.AuthorizedReq request, StreamObserver<DalUser.UserOrgsResp> responseObserver) {
        try {
            responseObserver.onNext(userService.readUserOrgs(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void createOrgAndUserOrg(DalUser.CreateOrgUserOrgReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.createOrgAndUserOrg(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateDepts(DalUser.DeptsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            userService.updateDepts(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readDepts(DalUser.ReadDeptsReq request, StreamObserver<CoreCommon.DeptsResp> responseObserver) {
        try {
            responseObserver.onNext(userService.readDepts(request).build());
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateMessages(DalUser.MessagesReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            userService.updateMessages(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readMessages(DalUser.ReadMessagesReq request, StreamObserver<CoreCommon.MessageResp> responseObserver) {
        try {
            if (request.getFromUserId() == 0L && request.getToUserId() == 0L)
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:from, to"));
            else {
                responseObserver.onNext(userService.readMessages(request).build());
                responseObserver.onCompleted();
            }
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateInbox(DalUser.UpdateInboxReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.updateInbox(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void deleteInbox(DalUser.UpdateInboxReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.deleteInbox(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateRelations(DalUser.UpdateRelationsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.updateRelations(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void deleteRelations(CoreCommon.AuthorizedIdsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.deleteRelations(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readRelations(DalUser.ReadRelationsReq request, StreamObserver<CoreCommon.RelationsResp> responseObserver) {
        try {
            CoreCommon.RelationsResp.Builder builder = CoreCommon.RelationsResp.newBuilder();
            long size = request.getPagination().getSize();
            if (size == 0L) size = 20L;
            QueryResults<Relation> res = userService.queryRelations(request.getRelation(), request.getPagination().getIdx(), size);
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
    public void readPartners(DalUser.ReadPartnersReq request, StreamObserver<CoreCommon.PartnersResp> responseObserver) {
        try {
            responseObserver.onNext(userService.readPartners(request).build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readPartnersIn(DalUser.ReadPartnersInReq request, StreamObserver<CoreCommon.PartnersResp> responseObserver) {
        try {
            responseObserver.onNext(userService.readPartnersIn(request).build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void createSimpleRelation(DalUser.SimpleRelationReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.createSimpleRelation(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void deleteSimpleRelation(DalUser.SimpleRelationReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.deleteSimpleRelation(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readSimpleRelations(DalUser.SimpleRelationReq request, StreamObserver<CoreCommon.SimpleRelationList> responseObserver) {
        try {
            if (request.getUserId() == 0L && request.getToId() == 0L) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:userId, toUserId"));
                return;
            }
            responseObserver.onNext(userService.readSimpleRelations(request));
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void updateDictItems(DalUser.DictItemsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.updateDictItems(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void readDictItems(DalUser.ReadDictItemsReq request, StreamObserver<CoreCommon.DictItemList> responseObserver) {
        try {
            responseObserver.onNext(userService.readDictItems(request));
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

    @Override
    public void deleteDictItems(CoreCommon.AuthorizedIdsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            userService.deleteDictItems(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }

}
