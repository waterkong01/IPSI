package kh.BackendCapstone.service;

import kh.BackendCapstone.entity.Member;
import kh.BackendCapstone.entity.PsContents;
import kh.BackendCapstone.entity.PsWrite;
import kh.BackendCapstone.repository.MemberRepository;
import kh.BackendCapstone.repository.PsContentsRepository;
import kh.BackendCapstone.repository.PsWriteRepository;
import kh.BackendCapstone.dto.request.PsWriteReqDto;
import kh.BackendCapstone.dto.request.PsContentsReqDto;
import kh.BackendCapstone.dto.response.PsWriteResDto;
import kh.BackendCapstone.dto.response.PsContentsResDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PsWriteService {
    private final MemberRepository memberRepository;
    private final PsWriteRepository psWriteRepository;
    private final PsContentsRepository psContentsRepository;
    private final MemberService memberService;
    
    @Transactional
    public PsWriteResDto savePsWrite(PsWriteReqDto psWriteReqDto, List<PsContentsReqDto> contentsReqDtoList, String token) {
        // 작성자 조회
        Member member = memberService.convertTokenToEntity(token);
        if(!member.getMemberId().equals(psWriteReqDto.getMemberId())) return null;
        PsWrite psWrite = null;
        if(psWriteReqDto.getPsWriteId() > 0) {
            psWrite = psWriteRepository.findByPsWriteId(psWriteReqDto.getPsWriteId())
                .orElseThrow(() -> new RuntimeException("해당 자소서가 없습니다."));
        }
        else {
            // 자기소개서 엔티티 생성
            psWrite = new PsWrite();
            psWrite.setMember(member);
            psWrite.setPsName(psWriteReqDto.getPsName());
            psWrite.setRegDate(LocalDateTime.now());
        }
        // 항목 리스트 저장
        PsWrite finalPsWrite = psWrite;
        
        List<PsContents> psContentsList = contentsReqDtoList.stream().map(contentDto -> {
            log.warn(contentDto.toString());
            boolean isExists = contentDto.getPsContentsId() > 0 && psContentsRepository.findByPsContentsId(contentDto.getPsContentsId())
                .orElseThrow(() -> new RuntimeException("해당 contents가 없습니다.")).getPsWrite().equals(finalPsWrite);
            
            PsContents psContents;
            if (isExists) {
                psContents = psContentsRepository.findByPsContentsId(contentDto.getPsContentsId())
                    .orElseThrow(() -> new RuntimeException("해당 contents가 없습니다."));
            } else {
                psContents = new PsContents(); // 새로운 객체 생성
            }
            psContents.setPsWrite(finalPsWrite);
            psContents.setPsTitle(contentDto.getPsTitle());
            psContents.setPsContent(contentDto.getPsContent());
            psContents.setSectionsNum(contentDto.getSectionsNum());
            return psContents;
        }).collect(Collectors.toList());

        psWrite.setPsContents(psContentsList);
        log.warn(psWrite.toString());
        // 저장
        PsWrite savedPsWrite = psWriteRepository.save(psWrite);
        // 저장된 데이터 DTO 변환 및 반환
        return convertToDto(savedPsWrite);
    }
    
    
    public PsWriteResDto loadPsWrite(Long psWriteId, String token) {
        try {
            PsWrite psWrite = psWriteRepository.findById(psWriteId)
                .orElseThrow(() -> new RuntimeException("해당 자소서가 없습니다."));
            Member member = memberService.convertTokenToEntity(token);
            if (psWrite.getMember().equals(member)) {
                PsWriteResDto psWriteResDto = convertToDto(psWrite);
                log.warn("작성한 자소서 번호 불러오기 :{}-{}", psWriteId, psWriteResDto);
                return psWriteResDto;
            }
            else return null;
        } catch (Exception e) {
            log.error("{}번 자소서를 불러오는 중 에러 : {}",psWriteId, e.getMessage());
            return null;
        }
    }
    
    public Long newPsWrite(String token) {
        Member member = memberService.convertTokenToEntity(token);
        PsWrite psWrite = new PsWrite();
        psWrite.setMember(member);
        psWrite.setPsName("새 자기소개서");
        psWrite.setRegDate(LocalDateTime.now());
        List<PsContents> psContentsList = new ArrayList<>();
        PsContents psContents = new PsContents();
        psContents.setPsWrite(psWrite);
        psContents.setSectionsNum(0);
        psContentsList.add(psContents);
        psWrite.setPsContents(psContentsList);
        psWriteRepository.save(psWrite);
        return psWrite.getPsWriteId();
    }

    // PsWrite 엔티티 PsWriteResDto 변환
    private PsWriteResDto convertToDto(PsWrite psWrite) {
        List<PsContentsResDto> contentsResDtos = psWrite.getPsContents().stream()
                .map(content -> new PsContentsResDto(content.getPsContentsId(), content.getPsTitle(), content.getPsContent()))
                .collect(Collectors.toList());

        return PsWriteResDto.builder()
                .psWriteId(psWrite.getPsWriteId())
                .memberId(psWrite.getMember().getMemberId())
                .psName(psWrite.getPsName())
                .regDate(psWrite.getRegDate())
                .psContents(contentsResDtos)
                .build();
    }
}