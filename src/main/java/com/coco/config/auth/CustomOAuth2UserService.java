package com.coco.config.auth;

import com.coco.config.auth.dto.OAuthAttributes;
import com.coco.config.auth.dto.SessionUser;
import com.coco.domain.user.User;
import com.coco.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate
                = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        /*
        * 현재 로그인 진행 중인 서비스를 구분하는 코드.
        * 구글/카카오/네이버 중 어떤 로그인을 사용했는지 구분하기 위함.
        * */
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        /*
        * OAuth2로그인 진행 시 키가 되는 필드값.(PK)
        * 구글 기본 코드 'sub'. 카카오나 네이버는 기본 값 지원하지 않음.
        * 네이버/구글 동시 지원 시 사용한다.
        * */
        String userNameAttributeName
            = userRequest.getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName();

        /*
        * OAuth2UserService를 통해 가져온 OAuth2User의 attribute를 담을 클래스
        * */
        OAuthAttributes attributes = OAuthAttributes
                .of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes);

        /*SessionUser
        * 세션에 사용자 정보를 저장하기 위한 Dto클래스
        * */
        httpSession.setAttribute("user", new SessionUser(user));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}
