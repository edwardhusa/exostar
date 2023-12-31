package com.husa.exostar.server.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class ExostarFileProcessResponse {

    private int linesInFile;

    private int linesParsed;

    private List<String> errors;

    public ExostarFileProcessResponse(String error) {
        errors = List.of(error);
    }

}
