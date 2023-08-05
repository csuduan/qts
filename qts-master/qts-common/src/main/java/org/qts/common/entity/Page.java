package org.qts.common.entity;

import lombok.Data;

import java.util.List;

@Data
public class Page<T> {
    private List<T> list;
    private int total;
}
