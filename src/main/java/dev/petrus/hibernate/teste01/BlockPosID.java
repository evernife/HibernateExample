package dev.petrus.hibernate.teste01;

import lombok.Data;

import javax.persistence.Embeddable;
import java.io.Serializable;


@Data
@Embeddable
public class BlockPosID implements Serializable {
    private Integer posX;
    private Integer posY;
    private Integer posZ;
}
