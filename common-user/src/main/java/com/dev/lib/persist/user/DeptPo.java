package com.dev.lib.persist.user;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sys_dept")
public class DeptPo extends JpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private DeptPo parent;

    @ManyToOne
    private UserPo manager;

    private Set<UserPo> users;
}
