package com.ntn.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ntn.auction.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {}
