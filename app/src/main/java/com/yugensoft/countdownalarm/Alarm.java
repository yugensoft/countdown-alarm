package com.yugensoft.countdownalarm;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class Alarm {
    // Schema area
    @Id(autoincrement = true)
    private Long id;
    // todo fill in


    // Generated & setter-getter area

    @Generated(hash = 481909998)
    public Alarm(Long id) {
        this.id = id;
    }

    @Generated(hash = 1972324134)
    public Alarm() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }


}
