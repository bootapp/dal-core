package com.bootapp.dal.core.service;

import com.bootapp.dal.core.domain.AuthorityOrg;
import com.bootapp.dal.core.domain.AuthorityUser;
import com.bootapp.dal.core.grpc.Auth;
import com.bootapp.dal.core.grpc.DalCoreAuthServiceGrpc;
import com.bootapp.dal.core.repository.AuthOrgRepository;
import com.bootapp.dal.core.repository.AuthUserRepository;
import com.bootapp.dal.core.utils.grpc.GrpcStatusException;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@GRpcService
public class AuthService extends DalCoreAuthServiceGrpc.DalCoreAuthServiceImplBase {
    private final AuthUserRepository authUserRepository;
    private final AuthOrgRepository authOrgRepository;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public AuthService(AuthUserRepository authUserRepository, AuthOrgRepository authOrgRepository) {
        this.authUserRepository = authUserRepository;
        this.authOrgRepository = authOrgRepository;
    }

    @Override
    public void getAuthorities(Empty request, StreamObserver<Auth.Authorities> responseObserver) {
        Auth.Authorities.Builder authorities = Auth.Authorities.newBuilder();
        try {
            List<AuthorityUser> userAuthorities = authUserRepository.findAll();
            List<AuthorityOrg> orgAuthorities = authOrgRepository.findAll();
            authorities.addAllOrgAuthorities(orgAuthorities.stream().map(AuthorityOrg::toProto).collect(Collectors.toList()));
            authorities.addAllUserAuthorities(userAuthorities.stream().map(AuthorityUser::toProto).collect(Collectors.toList()));
            responseObserver.onNext(authorities.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }
}
