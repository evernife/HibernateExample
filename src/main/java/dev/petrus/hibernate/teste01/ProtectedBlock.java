package dev.petrus.hibernate.teste01;

import lombok.Data;

import javax.persistence.*;

@Entity
@NamedQuery(name = "ProtectedBlock.getAll", query="SELECT p FROM ProtectedBlock p")
@NamedQuery(name = "ProtectedBlock.byTimeStart", query = "SELECT p FROM ProtectedBlock as p order by p.timeStart")
@Data
@Table(name = "protected_blocks", schema = "HibernateTest")
public class ProtectedBlock {

    @Basic
    @Column(name = "world")
    private String world;

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride( name = "posX", column = @Column(name = "pos_x")),
            @AttributeOverride( name = "posY", column = @Column(name = "pos_y")),
            @AttributeOverride( name = "posZ", column = @Column(name = "pos_z"))
    })
    private BlockPosID blockPosID = new BlockPosID();

    @Basic
    @Column(name = "time_start")
    private Long timeStart;

    @Basic
    @Column(name = "time_duration")
    private Long timeDuration;

}
