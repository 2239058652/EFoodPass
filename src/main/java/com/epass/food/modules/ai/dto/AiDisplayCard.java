package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiDisplayCard {

    private String title;

    private String type;

    private String summary;

    private List<AiDisplayField> fields;
}