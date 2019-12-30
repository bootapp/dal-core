package com.bootapp.core.controller;

import com.bootapp.core.domain.Authority;
import com.bootapp.core.domain.AuthorityGroup;
import com.bootapp.grpc.core.DalSysAuth;
import com.bootapp.grpc.core.DalSysAuthServiceGrpc;
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
public class SysAuthController extends DalSysAuthServiceGrpc.DalSysAuthServiceImplBase {
    private final AuthorityRepository authorityRepository;
    private final AuthorityGroupRepository authorityGroupRepository;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public SysAuthController(AuthorityRepository authorityRepository, AuthorityGroupRepository authOrgRepository) {
        this.authorityRepository = authorityRepository;
        this.authorityGroupRepository = authOrgRepository;
    }

    @Override
    public void readSysAuthorities(DalSysAuth.ReadSysAuthoritiesReq request, StreamObserver<DalSysAuth.SysAuthorities> responseObserver) {
        DalSysAuth.SysAuthorities.Builder resp = DalSysAuth.SysAuthorities.newBuilder();
        try {
            List<Authority> authorities = authorityRepository.findAll();
            List<AuthorityGroup> authorityGroups = authorityGroupRepository.findAll();
            resp.addAllAuthorityGroups(authorityGroups.stream().map(AuthorityGroup::toSysProto).collect(Collectors.toList()));
            resp.addAllAuthorities(authorities.stream().map(Authority::toSysProto).collect(Collectors.toList()));
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e.toString());
            responseObserver.onError(GrpcStatusException.GrpcInternalException(e));
        }
    }
}
