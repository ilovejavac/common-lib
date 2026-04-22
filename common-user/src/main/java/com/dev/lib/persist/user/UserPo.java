package com.dev.lib.persist.user;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class UserPo extends JpaEntity {


    @ManyToMany
    private Set<RolePo> roles = new HashSet<>();
}
