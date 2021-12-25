package dev.petrus.hibernate.teste02;

import lombok.*;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name="item")
@NamedQuery(name = "Item.getAll", query="SELECT i FROM Item i")
public class Item {

    @ManyToOne
    @JoinColumn(name="cart_id1", nullable=false)
    @JoinColumn(name="cart_id2", nullable=false)
    @JoinColumn(name="cart_id3", nullable=false)
    private Cart cart;

    @Id
    @Basic
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "item_id")
    private UUID id;

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                '}';
    }
}
