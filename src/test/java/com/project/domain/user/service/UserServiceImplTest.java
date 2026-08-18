package com.project.domain.user.service;

import com.project.common.exception.CustomException;
import com.project.common.util.S3Uploader;
import com.project.domain.user.domain.UserProfileVO;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.dto.UserProfileUpdateRequestDto;
import com.project.domain.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private UserServiceImpl userService;

    private UserProfileDto existingWithImage(String phone, String imageUrl) {
        return UserProfileDto.builder()
                .id(USER_ID)
                .name("홍길동")
                .role("G")
                .phone(phone)
                .profileImageUrl(imageUrl)
                .build();
    }

    private UserProfileUpdateRequestDto request() {
        return new UserProfileUpdateRequestDto();
    }

    // 1. 이미지 미전송 시 기존 이미지 유지
    @Test
    void keepsExistingImageWhenNoImageAndNoRemoveFlag() {
        UserProfileDto existing = existingWithImage("01011112222", "http://old-image");
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        userService.updateProfile(USER_ID, request());

        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).update(captor.capture());
        assertNull(captor.getValue().getProfileImage());
        verifyNoInteractions(s3Uploader);
    }

    // 회귀 테스트: PUT /profile은 is_shared를 건드리지 않아야 한다 (공유 여부는 PATCH /share 전용).
    // UserProfileUpdateRequestDto에는 shared 필드 자체가 없으므로, 정상 수정 시에도
    // VO의 isShared가 항상 null(=컬럼 미변경)로 전달되는지 검증한다.
    @Test
    void doesNotOverwriteSharedFlagOnProfileUpdate() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setName("새이름");

        userService.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).update(captor.capture());
        assertNull(captor.getValue().getIsShared());
    }

    // 2. 새 이미지 업로드 시 DB URL 변경 및 이전 이미지 삭제
    @Test
    void uploadsNewImageAndDeletesOldOne() {
        UserProfileDto existing = existingWithImage("01011112222", "http://old-image");
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(s3Uploader.upload(any(byte[].class), eq("profile-images"))).thenReturn("http://new-image");
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setProfileImage(new MockMultipartFile("profileImage", "a.png", "image/png", "data".getBytes()));

        userService.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).update(captor.capture());
        assertEquals("http://new-image", captor.getValue().getProfileImage());
        verify(s3Uploader).delete("http://old-image");
    }

    // 3. removeProfileImage=true 시 DB URL이 NULL로 변경되고 기존 이미지 삭제
    @Test
    void removesImageWhenRemoveFlagIsTrue() {
        UserProfileDto existing = existingWithImage("01011112222", "http://old-image");
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setRemoveProfileImage(true);

        userService.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).update(captor.capture());
        assertTrue(captor.getValue().getRemoveProfileImage());
        verify(s3Uploader).delete("http://old-image");
        verify(s3Uploader, never()).upload(any(byte[].class), any(String.class));
    }

    // 4. 이미지 파일과 삭제 플래그를 같이 보낼 때 400
    @Test
    void rejectsImageAndRemoveFlagTogether() {
        UserProfileDto existing = existingWithImage("01011112222", "http://old-image");
        when(userMapper.get(USER_ID)).thenReturn(existing);

        UserProfileUpdateRequestDto request = request();
        request.setRemoveProfileImage(true);
        request.setProfileImage(new MockMultipartFile("profileImage", "a.png", "image/png", "data".getBytes()));

        assertThrows(CustomException.class, () -> userService.updateProfile(USER_ID, request));

        verify(userMapper, never()).update(any(UserProfileVO.class));
        verifyNoInteractions(s3Uploader);
    }

    // 5. 비밀번호 변경 시 현재 비밀번호 일치 -> 성공
    @Test
    void changesPasswordWhenCurrentPasswordMatches() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(userMapper.getPasswordHash(USER_ID)).thenReturn("hashed-old");
        when(passwordEncoder.matches("oldPw", "hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("newPw")).thenReturn("hashed-new");
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setPassword("newPw");
        request.setCurrentPassword("oldPw");

        userService.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).update(captor.capture());
        assertEquals("hashed-new", captor.getValue().getPassword());
    }

    // 6. 현재 비밀번호 미입력 -> 400, 다른 값도 변경되지 않음
    @Test
    void rejectsPasswordChangeWithoutCurrentPassword() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing);

        UserProfileUpdateRequestDto request = request();
        request.setPassword("newPw");

        assertThrows(CustomException.class, () -> userService.updateProfile(USER_ID, request));

        verify(userMapper, never()).update(any(UserProfileVO.class));
    }

    // 6. 현재 비밀번호 불일치 -> 400, 다른 값도 변경되지 않음
    @Test
    void rejectsPasswordChangeWithWrongCurrentPassword() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing);
        when(userMapper.getPasswordHash(USER_ID)).thenReturn("hashed-old");
        when(passwordEncoder.matches("wrongPw", "hashed-old")).thenReturn(false);

        UserProfileUpdateRequestDto request = request();
        request.setPassword("newPw");
        request.setCurrentPassword("wrongPw");

        assertThrows(CustomException.class, () -> userService.updateProfile(USER_ID, request));

        verify(userMapper, never()).update(any(UserProfileVO.class));
    }

    // 7. 비밀번호 미변경 시 현재 비밀번호 없이 성공
    @Test
    void allowsUpdateWithoutPasswordChangeAndWithoutCurrentPassword() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setName("새이름");

        userService.updateProfile(USER_ID, request);

        verify(userMapper, never()).getPasswordHash(anyLong());
        verify(userMapper).update(any(UserProfileVO.class));
    }

    // 9. 전화번호 중복 확인: 본인 번호는 available=true
    @Test
    void ownPhoneIsAvailable() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing);
        when(userMapper.existsPhoneForOtherUser("01011112222", USER_ID)).thenReturn(false);

        assertTrue(userService.isPhoneAvailable(USER_ID, "01011112222"));
    }

    // 10. 전화번호 중복 확인: 타인 번호는 available=false
    @Test
    void otherUsersPhoneIsNotAvailable() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing);
        when(userMapper.existsPhoneForOtherUser("01099998888", USER_ID)).thenReturn(true);

        assertFalse(userService.isPhoneAvailable(USER_ID, "01099998888"));
    }

    // 11. 프로필 저장 중 중복 전화번호 변경 시 400
    @Test
    void rejectsProfileUpdateWithDuplicatePhone() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing);
        when(userMapper.existsPhoneForOtherUser("01099998888", USER_ID)).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setPhone("01099998888");

        assertThrows(CustomException.class, () -> userService.updateProfile(USER_ID, request));

        verify(userMapper, never()).update(any(UserProfileVO.class));
    }

    // 11-b. DB UNIQUE 제약 위반(동시 요청)도 400으로 변환
    @Test
    void translatesConcurrentDuplicatePhoneIntoBadRequest() {
        UserProfileDto existing = existingWithImage("01011112222", null);
        when(userMapper.get(USER_ID)).thenReturn(existing);
        when(userMapper.existsPhoneForOtherUser("01099998888", USER_ID)).thenReturn(false);
        when(userMapper.update(any(UserProfileVO.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        UserProfileUpdateRequestDto request = request();
        request.setPhone("01099998888");

        assertThrows(CustomException.class, () -> userService.updateProfile(USER_ID, request));
    }

    // 12. 전화번호의 하이픈 제거 후 중복 확인 및 저장
    @Test
    void normalizesPhoneHyphensBeforeSaving() {
        UserProfileDto existing = existingWithImage("01000000000", null);
        when(userMapper.get(USER_ID)).thenReturn(existing, existing);
        when(userMapper.existsPhoneForOtherUser("01012345678", USER_ID)).thenReturn(false);
        when(userMapper.update(any(UserProfileVO.class))).thenReturn(true);

        UserProfileUpdateRequestDto request = request();
        request.setPhone("010-1234-5678");

        userService.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).update(captor.capture());
        assertEquals("01012345678", captor.getValue().getPhone());
    }
}
