package com.dev.lib.persist.user;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sys_role")
public class RolePo extends JpaEntity {

    private String name;

    private String code;

    private Integer sorted;

    @OneToMany
    private Set<PermissionPo> permissions = new HashSet<>();

}
