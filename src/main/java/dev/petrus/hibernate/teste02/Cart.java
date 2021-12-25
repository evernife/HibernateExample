package dev.petrus.hibernate.teste02;


import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@Table(name = "cart")
@NamedQuery(name = "Cart.getAll", query="SELECT c FROM Cart c")
public class Cart implements Serializable {

    @Id
    @Basic
    @Column(name = "cart_id1")
    private UUID id1;

    @Id
    @Basic
    @Column(name = "cart_id2")
    private UUID id2;

    @Id
    @Basic
    @Column(name = "cart_id3")
    private UUID id3;

    @Basic
    @Column(name = "owner")
    private UUID owner;

    @OneToMany(mappedBy="cart", cascade = CascadeType.ALL)
    private Set<Item> itemSet = new HashSet<>();

    public void addToCart(Item item){
        item.setCart(this);
        if (itemSet == null) itemSet = new HashSet<>();
        itemSet.add(item);
    }

}
