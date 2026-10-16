package sn.uasz.xamle.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle.dto.SosRequest;
import sn.uasz.xamle.dto.SosResponse;
import sn.uasz.xamle.service.SosService;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService service;

    @PostMapping
    public SosResponse declencher(@Valid @RequestBody SosRequest req, HttpServletRequest http) {
        return service.declencher(req, http.getRemoteAddr());
    }
}