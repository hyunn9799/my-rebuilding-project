from app.ocr.services.text_normalizer import TextNormalizer


def test_text_normalizer_fixes_spaced_ocr_zero_in_dosage():
    normalizer = TextNormalizer()

    result = normalizer.normalize("타이레놀정 5 O O mg")

    assert result
    assert result[0].dosage == "500mg"
    assert result[0].name == "타이레놀정"
