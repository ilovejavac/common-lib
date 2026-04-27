package com.dev.lib.persist.user;

import com.dev.lib.biz.user.model.SystemUser;
import com.dev.lib.jpa.entity.JpaEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
@AutoMapper(target = SystemUser.class)
public class UserPo extends JpaEntity {

    @ManyToMany
    private Set<RolePo> roles = new HashSet<>();

}
