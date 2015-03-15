package org.monolith.exp;

import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="warehouses") //FIX FOR YOUR TABLE NAME
public class WritableObject {

    @Id
    @GeneratedValue
    private Long id = 1L;
    private String name = "name";

    @Temporal(TemporalType.DATE)
    private Date date;

    //private DateTime jodaDate;

//    public DateTime getJodaDate() {
//        return jodaDate;
//    }
//
//    public void setJodaDate(DateTime jodaDate) {
//        this.jodaDate = jodaDate;
//    }

    public Date getDate()
    {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WritableObject() {

    }

    @Override
    public String toString() {
        date = getDate();
        return this.getClass().getName() + "--" + name + "--" + id + "--" + date.toString();
    }

}
