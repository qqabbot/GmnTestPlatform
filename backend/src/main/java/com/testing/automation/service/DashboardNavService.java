package com.testing.automation.service;

import com.testing.automation.Mapper.NavAddressMapper;
import com.testing.automation.Mapper.NavEnvironmentMapper;
import com.testing.automation.model.NavAddress;
import com.testing.automation.model.NavEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardNavService {

    @Autowired
    private NavEnvironmentMapper navEnvironmentMapper;

    @Autowired
    private NavAddressMapper navAddressMapper;

    public List<NavEnvironment> getTree() {
        List<NavEnvironment> envs = navEnvironmentMapper.findAll();
        for (NavEnvironment env : envs) {
            List<NavAddress> addresses = navAddressMapper.findByEnvironmentId(env.getId());
            env.setAddresses(addresses);
        }
        return envs;
    }

    public List<NavEnvironment> getAllEnvironments() {
        return navEnvironmentMapper.findAll();
    }

    public NavEnvironment getEnvironmentById(Long id) {
        return navEnvironmentMapper.findById(id);
    }

    @Transactional
    public NavEnvironment createEnvironment(NavEnvironment env) {
        if (env.getSortOrder() == null) {
            env.setSortOrder(0);
        }
        env.setCreatedAt(LocalDateTime.now());
        env.setUpdatedAt(LocalDateTime.now());
        navEnvironmentMapper.insert(env);
        return env;
    }

    @Transactional
    public NavEnvironment updateEnvironment(Long id, NavEnvironment envDetails) {
        NavEnvironment env = navEnvironmentMapper.findById(id);
        if (env == null) {
            return null;
        }
        env.setName(envDetails.getName());
        env.setDescription(envDetails.getDescription());
        if (envDetails.getSortOrder() != null) {
            env.setSortOrder(envDetails.getSortOrder());
        }
        env.setUpdatedAt(LocalDateTime.now());
        navEnvironmentMapper.update(env);
        return env;
    }

    @Transactional
    public void deleteEnvironment(Long id) {
        navAddressMapper.deleteByEnvironmentId(id);
        navEnvironmentMapper.deleteById(id);
    }

    public List<NavAddress> getAddressesByEnvironmentId(Long environmentId) {
        return navAddressMapper.findByEnvironmentId(environmentId);
    }

    public NavAddress getAddressById(Long id) {
        return navAddressMapper.findById(id);
    }

    @Transactional
    public NavAddress createAddress(Long environmentId, NavAddress address) {
        address.setEnvironmentId(environmentId);
        if (address.getSortOrder() == null) {
            address.setSortOrder(0);
        }
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        navAddressMapper.insert(address);
        return address;
    }

    @Transactional
    public NavAddress updateAddress(Long id, NavAddress addressDetails) {
        NavAddress address = navAddressMapper.findById(id);
        if (address == null) {
            return null;
        }
        address.setShortName(addressDetails.getShortName());
        address.setLabel(addressDetails.getLabel());
        address.setUrl(addressDetails.getUrl());
        address.setRemark(addressDetails.getRemark());
        if (addressDetails.getSortOrder() != null) {
            address.setSortOrder(addressDetails.getSortOrder());
        }
        address.setUpdatedAt(LocalDateTime.now());
        navAddressMapper.update(address);
        return address;
    }

    @Transactional
    public void deleteAddress(Long id) {
        navAddressMapper.deleteById(id);
    }
}
