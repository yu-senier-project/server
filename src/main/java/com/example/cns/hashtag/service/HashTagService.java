package com.example.cns.hashtag.service;

import com.example.cns.common.exception.BusinessException;
import com.example.cns.common.exception.ExceptionCode;
import com.example.cns.feed.post.domain.Post;
import com.example.cns.feed.post.domain.repository.PostRepository;
import com.example.cns.hashtag.domain.HashTag;
import com.example.cns.hashtag.domain.HashTagPost;
import com.example.cns.hashtag.domain.HashTagPostId;
import com.example.cns.hashtag.domain.repository.HashTagPostRepository;
import com.example.cns.hashtag.domain.repository.HashTagRepository;
import com.example.cns.hashtag.dto.request.HashTagRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HashTagService {

    private final HashTagRepository hashTagRepository;
    private final PostRepository postRepository;
    private final HashTagPostRepository hashTagPostRepository;


    /*
    해시태그 검색
     */
    public List<HashTag> searchHashTag(String keyword){
        return hashTagRepository.findAllByNameContainingIgnoreCase(keyword);
    }

    /*
    해시태그 추가
    1. 해시 태그 추가
    2. 게시글과 해시태그 연결
     */
    @Transactional
    public void createHashTag(HashTagRequest hashTagRequest){

        //게시글 가져오기
        Optional<Post> post = postRepository.findById(hashTagRequest.postId());

        if(post.isPresent()){ // 게시글이 존재한다면

            List<HashTag> hashTags = new ArrayList<>(); //게시글에 추가할 해시태그 리스트

            //해시태그 request를 한개씩 확인하면서 존재하는지? 확인 후 추가
            hashTagRequest.hashTags().stream().forEach(requestHashTag -> {
                Optional<HashTag> hashTag = hashTagRepository.findByName(requestHashTag);
                if(hashTag.isEmpty()){ //해당하는 해시태그가 없을경우 생성후 선언
                    hashTag = Optional.of(hashTagRepository.save(HashTag.builder().name(requestHashTag).build()));
                }
                hashTags.add(hashTag.get()); //해시태그 리스트에 추가

                //해시태그 연관관계 테이블 추가
                HashTagPostId id = HashTagPostId.builder()
                        .hashtagId(hashTag.get().getId())
                        .postId(post.get().getId())
                        .build();

                HashTagPost hashTagPost = HashTagPost.builder()
                        .id(id)
                        .build();

                hashTagPostRepository.save(hashTagPost);

            });
        } else{ //게시글이 없다면 해시태그 없다고 선언
            throw new BusinessException(ExceptionCode.POST_NOT_EXIST);
        }
    }
    /*
    해시태그 삭제
    1. 게시글 관련 해시태그 삭제
    1-1. 단, 다른 게시글이 동일한 해시태그를 가지고있다면 삭제하면 안된다.
    2. 연관관계 테이블도 삭제
     */
    @Transactional
    public void deleteHashTag(Long postId){

        //게시글과 관련된 연관관계 데이터 가져오기
        List<HashTagPost> hashTagPostList = hashTagPostRepository.findAllByPostId(postId);

        hashTagPostList.stream().forEach(
                hashTagPost -> {
                    List<HashTagPost> hashTagPostListByHashTag = hashTagPostRepository.findAllByHashTagId(hashTagPost.getId().getHashtag());
                    if (hashTagPostListByHashTag.size() <= 1){ //연관된 게시글이 단 한개라면 테이블 삭제 + 해시태그 삭제
                        hashTagRepository.deleteById(hashTagPost.getId().getHashtag());
                    } //연관된 게시글이 더 있다면 테이블만 삭제
                    hashTagPostRepository.deleteById(hashTagPost.getId());
                }
        );
    }
}