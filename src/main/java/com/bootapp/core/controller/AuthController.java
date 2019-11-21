package com.bootapp.core.controller;

import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalAuth;
import com.bootapp.core.grpc.DalAuthServiceGrpc;
import com.bootapp.core.service.AuthService;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GRpcService
public class AuthController extends DalAuthServiceGrpc.DalAuthServiceImplBase {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void createAuthorities(DalAuth.AuthoritiesReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            authService.createAuthorities(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void updateAuthorities(DalAuth.AuthoritiesReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            authService.updateAuthorities(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void readAuthorities(DalAuth.ReadAuthoritiesReq request, StreamObserver<CoreCommon.AuthoritiesResp> responseObserver) {
        try {
            responseObserver.onNext(authService.readAuthorities(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void deleteAuthorities(DalAuth.DeleteAuthoritiesReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            authService.delAuthTx(request.getDataList());
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void updateAuthGroups(DalAuth.AuthGroupsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            authService.updateAuthGroups(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void readAuthGroups(DalAuth.ReadAuthGroupsReq request, StreamObserver<CoreCommon.AuthGroupsResp> responseObserver) {
        try {
            responseObserver.onNext(authService.readAuthGroups(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void deleteAuthGroups(CoreCommon.AuthorizedIdsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            authService.delAuthGroups(request.getIdsList());
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void updateRoleOrgs(DalAuth.RoleOrgsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0L) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            authService.updateRoleOrgs(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void readRoleOrgs(CoreCommon.AuthorizedPaginationReq request, StreamObserver<CoreCommon.RoleOrgsResp> responseObserver) {
        try {
            responseObserver.onNext(authService.readRoleOrgs(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void deleteRoleOrgs(CoreCommon.AuthorizedIdsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            authService.deleteRoleOrgs(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void updateRoleUsers(DalAuth.RoleUsersReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            if (request.getDataCount() <= 0L) {
                responseObserver.onError(GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:data"));
                return;
            }
            authService.updateRoleUsers(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void readRoleUsers(CoreCommon.AuthorizedPaginationReq request, StreamObserver<CoreCommon.RoleUsersResp> responseObserver) {
        try {
            responseObserver.onNext(authService.readRoleUsers(request).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
    @Override
    public void deleteRoleUsers(CoreCommon.AuthorizedIdsReq request, StreamObserver<CoreCommon.Empty> responseObserver) {
        try {
            authService.deleteRoleUsers(request);
            responseObserver.onNext(CoreCommon.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(e);
        }
    }
}
