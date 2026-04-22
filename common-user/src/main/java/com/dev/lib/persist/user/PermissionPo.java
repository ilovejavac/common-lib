package com.dev.lib.persist.user;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_permission")
public class PermissionPo extends JpaEntity {

    private String  code;

    private String  name;

    private Integer sorted;

}
