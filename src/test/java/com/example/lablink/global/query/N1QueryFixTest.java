package com.example.lablink.global.query;

import com.example.lablink.domain.application.entity.Application;
import com.example.lablink.domain.application.repository.ApplicationRepository;
import com.example.lablink.domain.application.service.ApplicationCompanyService;
import com.example.lablink.domain.bookmark.entity.Bookmark;
import com.example.lablink.domain.bookmark.repository.BookmarkRepository;
import com.example.lablink.domain.bookmark.service.BookmarkService;
import com.example.lablink.domain.chat.entity.ChatMessage;
import com.example.lablink.domain.chat.entity.ChatRoom;
import com.example.lablink.domain.chat.repository.ChatMessageRepository;
import com.example.lablink.domain.chat.repository.ChatRoomRepository;
import com.example.lablink.domain.chat.service.ChatService;
import com.example.lablink.domain.company.entity.Company;
import com.example.lablink.domain.company.security.CompanyDetailsImpl;
import com.example.lablink.domain.study.entity.Study;
import com.example.lablink.domain.study.service.GetStudyService;
import com.example.lablink.domain.study.service.StudyService;
import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserInfo;
import com.example.lablink.domain.user.security.UserDetailsImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class N1QueryFixTest {

    @Nested
    @DisplayName("BookmarkService — IN 쿼리로 Study 일괄 조회")
    class BookmarkServiceTest {
        @InjectMocks
        private BookmarkService bookmarkService;
        @Mock
        private BookmarkRepository bookmarkRepository;
        @Mock
        private GetStudyService getStudyService;

        @Test
        @DisplayName("getUserBookmark: getStudiesByIds 1회 호출, getStudy 0회")
        void getUserBookmark_batchQuery() {
            User user = new User();
            UserDetailsImpl userDetails = new UserDetailsImpl(user, "1");

            Bookmark b1 = new Bookmark(1L, user);
            Bookmark b2 = new Bookmark(2L, user);
            given(bookmarkRepository.findAllByUser(user)).willReturn(List.of(b1, b2));
            given(getStudyService.getStudiesByIds(anyList())).willReturn(Collections.emptyMap());

            bookmarkService.getUserBookmark(userDetails);

            verify(getStudyService, times(1)).getStudiesByIds(anyList());
            verify(getStudyService, never()).getStudy(anyLong());
        }
    }

    @Nested
    @DisplayName("ApplicationCompanyService — JOIN FETCH 쿼리 사용")
    class ApplicationCompanyServiceTest {
        @InjectMocks
        private ApplicationCompanyService applicationCompanyService;
        @Mock
        private ApplicationRepository applicationRepository;
        @Mock
        private GetStudyService getStudyService;
        @Mock
        private StudyService studyService;

        @Test
        @DisplayName("applicationFromStudy: findByStudyIdWithUserAndUserInfo 호출")
        void applicationFromStudy_joinFetch() {
            Company company = new Company();
            CompanyDetailsImpl companyDetails = new CompanyDetailsImpl(company, "1");
            Study study = new Study();

            given(studyService.findStudyFromCompany(anyLong(), any(Company.class))).willReturn(study);
            given(applicationRepository.findByStudyIdWithUserAndUserInfo(any())).willReturn(Collections.emptyList());

            applicationCompanyService.applicationFromStudy(companyDetails, 1L);

            verify(applicationRepository, times(1)).findByStudyIdWithUserAndUserInfo(any());
            verify(applicationRepository, never()).findByStudyId(anyLong());
        }
    }

    @Nested
    @DisplayName("ChatService — 일괄 마지막 메시지 조회")
    class ChatServiceTest {
        @InjectMocks
        private ChatService chatService;
        @Mock
        private ChatRoomRepository chatRoomRepository;
        @Mock
        private ChatMessageRepository chatMessageRepository;
        @Mock
        private GetStudyService getStudyService;
        @Mock
        private org.springframework.messaging.simp.SimpMessageSendingOperations template;
        @Mock
        private com.example.lablink.domain.user.service.UserService userService;

        @Test
        @DisplayName("findCompanyMessageHistory: findLastMessageByRooms 1회 호출, findLastMessageByRoom 0회")
        void findCompanyMessageHistory_batchQuery() {
            Company company = new Company();
            ChatRoom room = mock(ChatRoom.class);

            given(chatRoomRepository.findByRoomId("room1")).willReturn(Optional.of(room));
            given(chatMessageRepository.findAllByRoom(room)).willReturn(Collections.emptyList());
            given(chatRoomRepository.findAllChatRoomByCompany(company)).willReturn(Collections.emptyList());
            given(chatMessageRepository.findLastMessageByRooms(anyList())).willReturn(Collections.emptyList());

            chatService.findCompanyMessageHistory("room1", company);

            verify(chatMessageRepository, times(1)).findLastMessageByRooms(anyList());
            verify(chatMessageRepository, never()).findLastMessageByRoom(any(ChatRoom.class));
        }
    }
}
