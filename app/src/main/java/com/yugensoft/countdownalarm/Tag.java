package com.yugensoft.countdownalarm;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.converter.PropertyConverter;
import org.joda.time.MonthDay;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class Tag {
    // Schema area
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private long messageId;
    @NotNull
    @Convert(converter = TagTypeConverter.class, columnType = Integer.class)
    private TagType tagType;
    // Nullable
    private String compareDate; // In format DD MM, the day and month to countdown/up to; null for TODAYS_ types
    @NotNull
    private String speechFormat; // The way to read out the duration (for countup/down) or the date



    // Generated area -----------------

    @Generated(hash = 220103339)
    public Tag(Long id, long messageId, @NotNull TagType tagType, String compareDate,
            @NotNull String speechFormat) {
        this.id = id;
        this.messageId = messageId;
        this.tagType = tagType;
        this.compareDate = compareDate;
        this.speechFormat = speechFormat;
    }
    @Generated(hash = 1605720318)
    public Tag() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public long getMessageId() {
        return this.messageId;
    }
    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }
    public TagType getTagType() {
        return this.tagType;
    }
    public void setTagType(TagType tagType) {
        this.tagType = tagType;
    }
    public String getCompareDate() {
        return this.compareDate;
    }
    public void setCompareDate(String compareDate) {
        this.compareDate = compareDate;
    }
    public String getSpeechFormat() {
        return this.speechFormat;
    }
    public void setSpeechFormat(String speechFormat) {
        this.speechFormat = speechFormat;
    }


    // Converter area
    public enum TagType {
        UNKNOWN(0),
        COUNTDOWN(1),
        COUNTUP(2),
        TODAYS_DATE(3);

        final int id;

        TagType(int id){
            this.id = id;
        }
    }
    public static class TagTypeConverter implements PropertyConverter<TagType, Integer> {
        @Override
        public TagType convertToEntityProperty(Integer databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            for (TagType tagType : TagType.values()) {
                if (tagType.id == databaseValue) {
                    return tagType;
                }
            }
            // Unexpected tag type value
            return TagType.UNKNOWN;
        }

        @Override
        public Integer convertToDatabaseValue(TagType entityProperty) {
            return entityProperty == null ? null : entityProperty.id;
        }
    }
}
