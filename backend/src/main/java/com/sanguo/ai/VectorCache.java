package com.sanguo.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorCache {
    private String fingerprint;
    private List<Document> documents;
}
