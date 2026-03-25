package com.liriosbeauty.Controller;


import com.liriosbeauty.DTO.EmployeeBonusDTO;
import com.liriosbeauty.Service.BonusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bonus")
@RequiredArgsConstructor
public class BonusController {

    private final BonusService bonusService;

    @GetMapping("/quarterly")
    public ResponseEntity<List<EmployeeBonusDTO>> getQuarterlyBonus(
            @RequestParam int year,
            @RequestParam int quarter) {
        return ResponseEntity.ok(bonusService.calculateQuarterlyBonus(year, quarter));
    }
}
