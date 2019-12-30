package com.bootapp.core.service;

import com.bootapp.core.domain.*;
import com.bootapp.grpc.core.CoreCommon;
import com.bootapp.grpc.core.DalAuth;
import com.bootapp.core.repository.AuthorityGroupRepository;
import com.bootapp.core.repository.AuthorityRepository;
import com.bootapp.core.repository.RoleOrgRepository;
import com.bootapp.core.repository.RoleUserRepository;
import com.bootapp.core.utils.CommonUtils;
import com.bootapp.core.utils.grpc.GrpcStatusException;
import com.bootapp.core.utils.idgen.IDGenerator;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private JPAQueryFactory queryFactory;
    private final AuthorityRepository authorityRepository;
    private final AuthorityGroupRepository authorityGroupRepository;
    private final RoleOrgRepository roleOrgRepository;
    private final RoleUserRepository roleUserRepository;
    private final IDGenerator idGen;
    public AuthService(EntityManager em, AuthorityRepository authorityRepository, AuthorityGroupRepository authorityGroupRepository, RoleOrgRepository roleOrgRepository, RoleUserRepository roleUserRepository, IDGenerator idGen) {
        this.authorityRepository = authorityRepository;
        this.authorityGroupRepository = authorityGroupRepository;
        this.roleOrgRepository = roleOrgRepository;
        this.roleUserRepository = roleUserRepository;
        this.idGen = idGen;
        queryFactory = new JPAQueryFactory(em);
    }
    @Transactional
    public void createAuthorities(DalAuth.AuthoritiesReq request) {
        List<Authority> authToSave = new ArrayList<>();
        for(CoreCommon.AuthorityEdit x: request.getDataList()) {
            Authority auth = new Authority();
            auth.fromProto(x);
            authToSave.add(auth);
            if (authorityRepository.findOneByGroupIdAndValue(auth.getGroupId(), auth.getValue()).isPresent())
                throw GrpcStatusException.GrpcAlreadyExistsException("ALREADY_EXISTS:value");
        }
        authorityRepository.saveAll(authToSave);
    }
    @Transactional
    public void updateAuthorities(DalAuth.AuthoritiesReq request) {
        Map<String, CoreCommon.AuthorityEdit> authorityMap = request.getDataList().stream()
                .collect(Collectors.toMap(CoreCommon.AuthorityEdit::getKey, x -> x));
        List<Authority> results = authorityRepository.findAllById(authorityMap.keySet());
        results.forEach( x -> x.fromProto(authorityMap.get(x.getKey())));
        authorityRepository.saveAll(results);
    }
    public CoreCommon.AuthoritiesResp.Builder readAuthorities(DalAuth.ReadAuthoritiesReq request) {
        QAuthority qAuthority = QAuthority.authority;
        List<Authority> res = queryFactory.selectFrom(qAuthority)
                .where(qAuthority.groupId.eq(request.getGroupId())).orderBy(qAuthority.value.asc()).fetch();
        CoreCommon.AuthoritiesResp.Builder resp = CoreCommon.AuthoritiesResp.newBuilder();
        resp.addAllData(res.stream().map(Authority::toProto).collect(Collectors.toList()));
        return resp;
    }
    @Transactional
    public void delAuthTx(List<String> ids) {
        authorityRepository.deleteByKeyIn(ids);
    }

    @Transactional
    public void updateAuthGroups(DalAuth.AuthGroupsReq request) {
        List<AuthorityGroup> authGroupsToSave = new ArrayList<>();
        Map<Long, CoreCommon.AuthGroupEdit> authorityGroupMap = new HashMap<>();
        for(CoreCommon.AuthGroupEdit group : request.getDataList()) {
            if (group.getId() == 0L) {
                AuthorityGroup aGroup = new AuthorityGroup();
                aGroup.fromProto(group);
                authGroupsToSave.add(aGroup);
            } else
                authorityGroupMap.put(group.getId(), group);
        }
        List<AuthorityGroup> res = authorityGroupRepository.findAllById(authorityGroupMap.keySet());
        res.forEach(x -> {
            x.fromProto(authorityGroupMap.get(x.getId()));
            authGroupsToSave.add(x);
        });

        for (AuthorityGroup g : authGroupsToSave) {
            Optional<AuthorityGroup> pGroupOptional = authorityGroupRepository.findById(g.getPid());
            if (!pGroupOptional.isPresent())
                throw GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:pid non-exists");
                else {
                if (pGroupOptional.get().getPid() != 0L)
                    throw GrpcStatusException.GrpcInvalidArgException("INVALID_ARG:pid nesting");
            }
        }
        authorityGroupRepository.saveAll(authGroupsToSave);
    }
    public CoreCommon.AuthGroupsResp.Builder readAuthGroups(DalAuth.ReadAuthGroupsReq request) {
        List<AuthorityGroup> result = authorityGroupRepository.findAllByPid(request.getPid());
        CoreCommon.AuthGroupsResp.Builder resp = CoreCommon.AuthGroupsResp.newBuilder();
        resp.addAllData(result.stream().map(AuthorityGroup::toProto).collect(Collectors.toList()));
        return resp;
    }
    @Transactional
    public void delAuthGroups(List<Long> ids) {
        authorityGroupRepository.deleteAllByIdIn(ids);
    }
    @Transactional
    public void updateRoleOrgs(DalAuth.RoleOrgsReq request) {
        List<RoleOrg> roleOrgsToSave = new ArrayList<>();
        Map<Long, CoreCommon.RoleOrgEdit> roleOrgEditMap = new HashMap<>();
        for(CoreCommon.RoleOrgEdit edit : request.getDataList()) {
            if (edit.getId() == 0L) {
                RoleOrg roleOrg = new RoleOrg();
                roleOrg.setId(idGen.nextId());
                if (edit.hasName()) roleOrg.setName(edit.getName().getValue());
                if (edit.hasRemark()) roleOrg.setRemark(edit.getRemark().getValue());
                if (edit.hasAuthorities()) roleOrg.setAuthorities(edit.getAuthorities().getValue());
                roleOrgsToSave.add(roleOrg);
            } else {
                roleOrgEditMap.put(edit.getId(), edit);
            }
        }
        List<RoleOrg> roleOrgList = roleOrgRepository.findAllById(roleOrgEditMap.keySet());
        roleOrgList.forEach(x -> {
            x.fromProto(roleOrgEditMap.get(x.getId()));
            roleOrgsToSave.add(x);
        });
        roleOrgRepository.saveAll(roleOrgsToSave);
    }
    public CoreCommon.RoleOrgsResp.Builder readRoleOrgs(CoreCommon.AuthorizedPaginationReq request) {
        QRoleOrg qRoleOrg = QRoleOrg.roleOrg;
        long limit = request.getPagination().getSize();
        if (limit <= 0) limit = 20;
        QueryResults<RoleOrg> results = queryFactory.selectFrom(qRoleOrg).offset(request.getPagination().getIdx()).limit(limit).fetchResults();
        CoreCommon.RoleOrgsResp.Builder resp = CoreCommon.RoleOrgsResp.newBuilder();
        resp.addAllData(results.getResults().stream().map(RoleOrg::toProto).collect(Collectors.toList()));
        resp.setPagination(CommonUtils.buildPagination(results.getOffset(), results.getLimit(), results.getTotal()));
        return resp;
    }
    @Transactional
    public void deleteRoleOrgs(CoreCommon.AuthorizedIdsReq request) {
        roleOrgRepository.deleteAllByIdIn(request.getIdsList());
    }
    @Transactional
    public void updateRoleUsers(DalAuth.RoleUsersReq request) {
        List<RoleUser> roleUsersToSave = new ArrayList<>();
        Map<Long, CoreCommon.RoleUserEdit> roleUserMap = new HashMap<>();
        for (CoreCommon.RoleUserEdit edit : request.getDataList()) {
            if (edit.getId() == 0L) {
                RoleUser roleUser = new RoleUser();
                roleUser.setId(idGen.nextId());
                roleUser.setOrgId(request.getOrgId());
                roleUser.fromProto(edit);
                roleUsersToSave.add(roleUser);
            } else {
                roleUserMap.put(edit.getId(), edit);
            }
        }
        List<RoleUser> roleUsers = roleUserRepository.findAllById(roleUserMap.keySet());
        roleUsers.forEach(x -> {
            x.fromProto(roleUserMap.get(x.getId()));
            roleUsersToSave.add(x);
        });
        roleUserRepository.saveAll(roleUsersToSave);
    }
    public CoreCommon.RoleUsersResp.Builder readRoleUsers(CoreCommon.AuthorizedPaginationReq request) {
        long limit = request.getPagination().getSize();
        if (limit <= 0) limit = 20;
        QRoleUser qRoleUser = QRoleUser.roleUser;
        QueryResults<RoleUser> results = queryFactory.selectFrom(qRoleUser)
                .where(qRoleUser.orgId.eq(request.getOrgId()))
                .offset(request.getPagination().getIdx()).limit(limit).fetchResults();
        CoreCommon.RoleUsersResp.Builder resp = CoreCommon.RoleUsersResp.newBuilder();
        resp.addAllData(results.getResults().stream().map(RoleUser::toProto).collect(Collectors.toList()));
        resp.setPagination(CommonUtils.buildPagination(results.getOffset(), results.getLimit(), results.getTotal()));
        return resp;
    }
    @Transactional
    public void deleteRoleUsers(CoreCommon.AuthorizedIdsReq request) {
        roleUserRepository.deleteAllByIdIn(request.getIdsList());
    }
}
