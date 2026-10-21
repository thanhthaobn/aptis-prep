-- Original teaching scripts rendered with Microsoft David Desktop (Windows SAPI).
-- Audio is bundled with the application and served through the authenticated lesson media endpoint.
UPDATE lessons SET audio_path='curriculum/audio/listening-a2.wav',
 audio_name=N'Bài nghe luyện tập A2 · Giọng tổng hợp.wav'
WHERE title=N'Nghe thông báo thay đổi lịch trình' AND skill='Listening' AND cefr_level='A2' AND audio_path IS NULL;

UPDATE lessons SET audio_path='curriculum/audio/listening-b1.wav',
 audio_name=N'Bài nghe luyện tập B1 · Giọng tổng hợp.wav'
WHERE title=N'Ghép người nói với lý do lựa chọn' AND skill='Listening' AND cefr_level='B1' AND audio_path IS NULL;

UPDATE lessons SET audio_path='curriculum/audio/listening-b2.wav',
 audio_name=N'Bài nghe luyện tập B2 · Giọng tổng hợp.wav'
WHERE title=N'Theo dõi sự đồng ý có điều kiện trong thảo luận' AND skill='Listening' AND cefr_level='B2' AND audio_path IS NULL;

UPDATE lessons SET audio_path='curriculum/audio/listening-c1.wav',
 audio_name=N'Bài nghe luyện tập C1 · Giọng tổng hợp.wav'
WHERE title=N'Nghe sự dè dặt và hàm ý trong một bài bình luận' AND skill='Listening' AND cefr_level='C1' AND audio_path IS NULL;

