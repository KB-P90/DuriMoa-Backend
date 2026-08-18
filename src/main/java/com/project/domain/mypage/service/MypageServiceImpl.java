package com.project.domain.mypage.service;

import com.project.domain.mypage.dto.LinkedAccountDTO;
import com.project.domain.mypage.dto.LinkedAssetsResponseDTO;
import com.project.domain.mypage.dto.LinkedCardDTO;
import com.project.domain.mypage.mapper.MypageMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class MypageServiceImpl implements MypageService {

    private static final int VISIBLE_DIGITS = 4;

    private final MypageMapper mypageMapper;

    @Override
    public LinkedAssetsResponseDTO getLinkedAssets(Long userId) {
        List<LinkedAccountDTO> accounts = mypageMapper.selectLinkedAccounts(userId);
        accounts.forEach(this::maskAccountNumber);

        List<LinkedCardDTO> cards = mypageMapper.selectLinkedCards(userId);

        return LinkedAssetsResponseDTO.builder()
                .accounts(accounts)
                .cards(cards)
                .build();
    }

    private void maskAccountNumber(LinkedAccountDTO account) {
        account.setMaskedNumber(mask(account.getNumber()));
        account.setNumber(null);
    }

    private String mask(String number) {
        if (number == null || number.length() <= VISIBLE_DIGITS) {
            return number;
        }

        int maskLength = number.length() - VISIBLE_DIGITS;
        StringBuilder masked = new StringBuilder(number.length());
        for (int i = 0; i < maskLength; i++) {
            masked.append(number.charAt(i) == '-' ? '-' : '*');
        }
        masked.append(number, maskLength, number.length());

        return masked.toString();
    }
}
