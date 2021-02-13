package com.able.model;



import java.io.Serializable;
import java.util.Date;
import java.util.StringJoiner;


public class User implements Serializable {

    private Integer id;
    private String name;
    private Date createTime;
    private Date updateTime;
    private Long updateLongTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateLongTime() {
        return updateLongTime;
    }

    public void setUpdateLongTime(Long updateLongTime) {
        this.updateLongTime = updateLongTime;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", User.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("createTime=" + createTime)
                .add("updateTime=" + updateTime)
                .add("updateLongTime=" + updateLongTime)
                .toString();
    }
}
