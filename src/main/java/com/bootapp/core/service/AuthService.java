package com.bootapp.core.service;

import com.bootapp.core.domain.Authority;
import com.bootapp.core.domain.AuthorityGroups;
import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalAuth;
import com.bootapp.core.grpc.DalAuthServiceGrpc;
import com.bootapp.core.repository.AuthorityGroupRepository;
import com.bootapp.core.repository.AuthorityRepository;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@GRpcService
public class AuthService extends DalAuthServiceGrpc.DalAuthServiceImplBase {
    private final AuthorityRepository authorityRepository;
    private final AuthorityGroupRepository authorityGroupRepository;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public AuthService(AuthorityRepository authorityRepository, AuthorityGroupRepository authOrgRepository) {
        this.authorityRepository = authorityRepository;
        this.authorityGroupRepository = authOrgRepository;
    }

    @Override
    public void readAuthorities(DalAuth.AuthEmpty request, StreamObserver<DalAuth.Authorities> responseObserver) {
        DalAuth.Authorities.Builder resp = DalAuth.Authorities.newBuilder();
        try {
            List<Authority> authorities = authorityRepository.findAll();
            List<AuthorityGroups> authorityGroups = authorityGroupRepository.findAll();
            resp.addAllAuthorityGroups(authorityGroups.stream().map(AuthorityGroups::toProto).collect(Collectors.toList()));
            resp.addAllAuthorities(authorities.stream().map(Authority::toProto).collect(Collectors.toList()));
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }
}
