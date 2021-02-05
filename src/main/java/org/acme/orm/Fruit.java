package org.acme.orm;

import javax.persistence.*;

@Entity
@Table(name = "known_fruits")
@NamedQuery(name = "Fruits.findAll", query = "SELECT f FROM Fruit f ORDER BY f.name", hints = @QueryHint(name = "org.hibernate.cacheable", value = "true"))
public class Fruit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "known_fruits_seq")
    @SequenceGenerator(
            name="known_fruits_seq",
            sequenceName="i_known_fruits_sequence",
            allocationSize=1000,
            initialValue=10)
    private Integer id;

    @Column(length = 40)
    private String name;

    public Fruit() {
    }

    public Fruit(String name) {
        this.name = name;
    }

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

}
