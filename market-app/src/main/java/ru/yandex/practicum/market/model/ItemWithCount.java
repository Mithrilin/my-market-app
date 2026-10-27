package ru.yandex.practicum.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Column;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class ItemWithCount {

    private Long id;

    private String title;

    private String description;

    @Column("img_path")
    private String imgPath;

    private Long price;

    private Integer count;
}