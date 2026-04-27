import { useState, useCallback } from "react";
import { startPasskeyLogin, finishPasskeyLogin, startPasskeyRegistration, finishPasskeyRegistration } from "@/api/passkey";
import { setAccessToken } from "@/api/index";

// WebAuthn 지원 여부 확인
export const isWebAuthnSupported = () => {
  return !!(
    window.PublicKeyCredential &&
    navigator.credentials &&
    navigator.credentials.create &&
    navigator.credentials.get
  );
};

// 플랫폼 인증기(지문, Face ID 등) 지원 여부 확인
export const isPlatformAuthenticatorAvailable = async (): Promise<boolean> => {
  if (!isWebAuthnSupported()) return false;
  try {
    return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
  } catch {
    return false;
  }
};

interface UseWebAuthnReturn {
  isSupported: boolean;
  isPlatformAvailable: boolean | null;
  isRegistering: boolean;
  isAuthenticating: boolean;
  error: string | null;
  register: () => Promise<boolean>;  // ✅ userId 파라미터 제거
  authenticate: () => Promise<{ success: boolean; accessToken?: string; role?: string }>;
  checkPlatformAuthenticator: () => Promise<void>;
}

export const useWebAuthn = (): UseWebAuthnReturn => {
  const [isSupported] = useState(() => isWebAuthnSupported());
  const [isPlatformAvailable, setIsPlatformAvailable] = useState<boolean | null>(null);
  const [isRegistering, setIsRegistering] = useState(false);
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const checkPlatformAuthenticator = useCallback(async () => {
    const available = await isPlatformAuthenticatorAvailable();
    setIsPlatformAvailable(available);
  }, []);

  // 지문/생체 인증 등록 (백엔드 API 연동)
  // ✅ 보안 강화: userId는 JWT에서 자동 추출되므로 파라미터로 받지 않음
  const register = useCallback(async (): Promise<boolean> => {
    if (!isWebAuthnSupported()) {
      setError("이 기기에서는 생체 인증을 지원하지 않습니다.");
      return false;
    }

    setIsRegistering(true);
    setError(null);

    try {
      // 1. 서버에서 등록 옵션 가져오기 (userId는 JWT에서 자동 추출)
      const startResponse = await startPasskeyRegistration();
      
      // 응답 검증
      if (!startResponse.creationOptionsJson) {
        setError("서버 응답이 올바르지 않습니다.");
        return false;
      }
      
      let creationOptions;
      try {
        const parsed = JSON.parse(startResponse.creationOptionsJson);
        console.log('[WebAuthn] Parsed registration response:', parsed);
        
        // 백엔드가 중첩된 구조로 보내는 경우 처리
        if (parsed.publicKeyCredentialCreationOptions) {
          creationOptions = parsed.publicKeyCredentialCreationOptions;
          console.log('[WebAuthn] Using nested publicKeyCredentialCreationOptions');
        } else {
          creationOptions = parsed;
        }
        
        console.log('[WebAuthn] Final creation options:', creationOptions);
        console.log('[WebAuthn] RP ID from backend:', creationOptions.rp?.id);
        console.log('[WebAuthn] Current domain:', window.location.hostname);
      } catch (parseError) {
        console.error('[WebAuthn] Failed to parse creationOptionsJson:', parseError);
        setError("서버 응답 형식이 올바르지 않습니다.");
        return false;
      }

      // challenge와 user.id 검증 및 변환
      if (!creationOptions.challenge) {
        setError("인증 챌린지가 없습니다.");
        return false;
      }
      if (!creationOptions.user?.id) {
        setError("사용자 정보가 올바르지 않습니다.");
        return false;
      }
      
      creationOptions.challenge = base64URLDecode(creationOptions.challenge);
      creationOptions.user.id = base64URLDecode(creationOptions.user.id);

      if (creationOptions.excludeCredentials) {
        creationOptions.excludeCredentials = creationOptions.excludeCredentials
          .filter((cred: any) => cred && cred.id) // id가 있는 것만 필터링
          .map((cred: any) => ({
            ...cred,
            id: base64URLDecode(cred.id),
          }));
      }

      // 2. 브라우저가 "지문 대세요" 창을 띄움
      const credential = await navigator.credentials.create({
        publicKey: creationOptions,
      }) as PublicKeyCredential;

      if (!credential) {
        setError("인증 정보를 생성하지 못했습니다.");
        return false;
      }

      // 3. 서버에 등록 완료 요청 (userId는 JWT에서 자동 추출)
      const attestationResponse = credential.response as AuthenticatorAttestationResponse;
      const credentialJson = JSON.stringify({
        id: credential.id,
        rawId: base64URLEncode(credential.rawId),
        response: {
          attestationObject: base64URLEncode(attestationResponse.attestationObject),
          clientDataJSON: base64URLEncode(attestationResponse.clientDataJSON),
        },
        type: credential.type,
        clientExtensionResults: credential.getClientExtensionResults(),
      });

      await finishPasskeyRegistration(startResponse.requestId, credentialJson);
      return true;

    } catch (err: any) {
      console.error("Registration error:", err);
      
      // 백엔드 에러 처리
      if (err.response) {
        const status = err.response.status;
        const errorData = err.response.data;
        
        if (status === 401 || status === 403) {
          setError("로그인이 필요합니다. 다시 로그인해주세요.");
        } else if (status === 500) {
          console.error("Server error details:", errorData);
          setError("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } else if (errorData?.message) {
          setError(errorData.message);
        } else {
          setError("등록 중 오류가 발생했습니다.");
        }
      } else if (err instanceof Error) {
        // 브라우저 WebAuthn 에러
        if (err.name === "NotAllowedError") {
          setError("인증이 취소되었습니다. 다시 시도해주세요.");
        } else if (err.name === "NotSupportedError") {
          setError("이 기기에서는 생체 인증을 지원하지 않습니다.");
        } else {
          setError("등록 중 오류가 발생했습니다: " + err.message);
        }
      } else {
        setError("등록 중 오류가 발생했습니다.");
      }
      return false;
    } finally {
      setIsRegistering(false);
    }
  }, []);

  // 지문/생체 인증 로그인 (백엔드 API 연동)
  // 새 응답: 토큰 + 사용자 프로필 함께 반환 (추가 API 호출 불필요)
  const authenticate = useCallback(async (): Promise<{
    success: boolean;
    accessToken?: string;
    user?: { id: number; name: string; phone: string; role: string; };
  }> => {
    if (!isWebAuthnSupported()) {
      setError("이 기기에서는 생체 인증을 지원하지 않습니다.");
      return { success: false };
    }

    setIsAuthenticating(true);
    setError(null);

    try {
      // 1. 서버에서 인증 옵션 가져오기
      console.log('[WebAuthn] Starting passkey login...');
      const startResponse = await startPasskeyLogin();
      console.log('[WebAuthn] Start response:', startResponse);
      
      // 응답 검증 - 더 명확한 에러 메시지
      if (!startResponse) {
        console.error('[WebAuthn] No response from server');
        setError("서버와 연결할 수 없습니다. 네트워크를 확인해주세요.");
        return { success: false };
      }
      
      if (!startResponse.assertionRequestJson) {
        console.error('[WebAuthn] Missing assertionRequestJson in response:', startResponse);
        setError("서버 응답이 올바르지 않습니다. 관리자에게 문의해주세요.");
        return { success: false };
      }
      
      let assertionOptions;
      try {
        const parsed = JSON.parse(startResponse.assertionRequestJson);
        console.log('[WebAuthn] Parsed response:', parsed);
        
        // 백엔드가 중첩된 구조로 보내는 경우 처리
        if (parsed.publicKeyCredentialRequestOptions) {
          assertionOptions = parsed.publicKeyCredentialRequestOptions;
          console.log('[WebAuthn] Using nested publicKeyCredentialRequestOptions');
        } else {
          assertionOptions = parsed;
        }
        
        console.log('[WebAuthn] Final assertion options:', assertionOptions);
        console.log('[WebAuthn] Challenge value:', assertionOptions?.challenge);
      } catch (parseError) {
        console.error('[WebAuthn] Failed to parse assertionRequestJson:', parseError);
        setError("서버 응답 형식이 올바르지 않습니다.");
        return { success: false };
      }

      // challenge 검증 및 변환
      if (!assertionOptions.challenge) {
        console.error('[WebAuthn] Missing challenge in assertion options');
        console.error('[WebAuthn] Full assertion options:', JSON.stringify(assertionOptions, null, 2));
        setError("인증 챌린지가 없습니다. 다시 시도해주세요.");
        return { success: false };
      }
      
      try {
        assertionOptions.challenge = base64URLDecode(assertionOptions.challenge);
      } catch (decodeError) {
        console.error('[WebAuthn] Failed to decode challenge:', decodeError);
        setError("인증 데이터 처리 중 오류가 발생했습니다.");
        return { success: false };
      }

      // allowCredentials 처리 - 등록된 패스키가 없으면 에러
      if (assertionOptions.allowCredentials && assertionOptions.allowCredentials.length > 0) {
        assertionOptions.allowCredentials = assertionOptions.allowCredentials
          .filter((cred: any) => cred && cred.id) // id가 있는 것만 필터링
          .map((cred: any) => ({
            ...cred,
            id: base64URLDecode(cred.id),
          }));

        // 필터링 후 빈 배열이면 등록된 패스키 없음
        if (assertionOptions.allowCredentials.length === 0) {
          setError("등록된 생체 인증 정보가 없습니다. 먼저 등록해주세요.");
          return { success: false };
        }
      } else {
        // allowCredentials가 없거나 비어있으면 - Passkey Discoverable Credentials 시도
        // 이 경우 브라우저가 저장된 모든 패스키를 검색
      }

      // 2. 브라우저가 "저장된 패스키로 로그인할까요?" 창을 띄움
      const credential = await navigator.credentials.get({
        publicKey: assertionOptions,
      }) as PublicKeyCredential;

      if (!credential) {
        setError("인증 정보를 가져오지 못했습니다.");
        return { success: false };
      }

      // 3. 서버에 인증 완료 요청 및 토큰 + 사용자 프로필 발급 받기
      const assertionResponse = credential.response as AuthenticatorAssertionResponse;
      const credentialJson = JSON.stringify({
        id: credential.id,
        rawId: base64URLEncode(credential.rawId),
        response: {
          authenticatorData: base64URLEncode(assertionResponse.authenticatorData),
          clientDataJSON: base64URLEncode(assertionResponse.clientDataJSON),
          signature: base64URLEncode(assertionResponse.signature),
          userHandle: assertionResponse.userHandle ? base64URLEncode(assertionResponse.userHandle) : null,
        },
        type: credential.type,
        clientExtensionResults: credential.getClientExtensionResults(),
      });

      // Note: backend uses 'requsetId' (typo)
      const loginResponse = await finishPasskeyLogin(startResponse.requsetId, credentialJson);

      // 토큰 저장
      setAccessToken(loginResponse.accessToken);

      // 성공: 토큰 + 사용자 프로필 반환 (추가 API 호출 불필요!)
      return {
        success: true,
        accessToken: loginResponse.accessToken,
        user: loginResponse.user,
      };

    } catch (err: any) {
      console.error("Authentication error:", err);
      
      // 백엔드 에러 처리
      if (err.response) {
        const status = err.response.status;
        const errorData = err.response.data;
        const errorCode = errorData?.error;
        
        console.log('[WebAuthn] Backend error:', { status, errorCode, errorData });
        
        if (status === 401 || status === 403) {
          setError("인증에 실패했습니다. 다시 시도해주세요.");
        } else if (errorCode === 'WEBAUTHN_LOGIN_FAILED') {
          // Unknown credential 에러
          if (errorData?.message?.includes('Unknown credential')) {
            setError("등록된 생체 인증 정보가 없습니다. 먼저 로그인 후 프로필에서 지문을 등록해주세요.");
          } else {
            setError("생체 인증에 실패했습니다. 다시 시도해주세요.");
          }
        } else if (status === 500) {
          console.error("Server error details:", errorData);
          setError("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } else if (errorData?.message) {
          setError(errorData.message);
        } else {
          setError("로그인 중 오류가 발생했습니다.");
        }
      } else if (err instanceof Error) {
        // 브라우저 WebAuthn 에러
        if (err.name === "NotAllowedError") {
          setError("인증이 취소되었습니다. 다시 시도해주세요.");
        } else if (err.name === "NotSupportedError") {
          setError("이 기기에서는 생체 인증을 지원하지 않습니다.");
        } else if (err.message?.includes("WEBAUTHN")) {
          // 백엔드 에러
          if (err.message.includes("CRED_NOT_FOUND")) {
            setError("등록된 생체 인증 정보가 없습니다. 먼저 등록해주세요.");
          } else if (err.message.includes("AUTH_REQUEST_EXPIRED")) {
            setError("인증 요청이 만료되었습니다. 다시 시도해주세요.");
          } else {
            setError("로그인 중 오류가 발생했습니다.");
          }
        } else {
          setError("로그인 중 오류가 발생했습니다: " + err.message);
        }
      } else {
        setError("로그인 중 오류가 발생했습니다.");
      }
      return { success: false };
    } finally {
      setIsAuthenticating(false);
    }
  }, []);

  return {
    isSupported,
    isPlatformAvailable,
    isRegistering,
    isAuthenticating,
    error,
    register,
    authenticate,
    checkPlatformAuthenticator,
  };
};

// Base64URL 인코딩/디코딩 유틸리티
const base64URLEncode = (buffer: ArrayBuffer): string => {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
};

const base64URLDecode = (base64url: string): ArrayBuffer => {
  // 방어 코드: undefined나 null 체크
  if (!base64url || typeof base64url !== 'string') {
    console.error('base64URLDecode: Invalid input', base64url);
    throw new Error('Invalid base64url string');
  }
  
  const base64 = base64url.replace(/-/g, "+").replace(/_/g, "/");
  const padding = "=".repeat((4 - (base64.length % 4)) % 4);
  const binary = atob(base64 + padding);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
};
