import socket
import os
import datetime

# 서버 설정
HOST = '127.0.0.1'  # 로컬호스트
PORT = 8000  # 사용할 포트 번호

# 요청 데이터를 저장할 폴더
REQUEST_FOLDER = 'request'
IMAGE_FOLDER = 'images'

# 폴더가 존재하지 않으면 생성
os.makedirs(REQUEST_FOLDER, exist_ok=True)
os.makedirs(IMAGE_FOLDER, exist_ok=True)


def save_request_data(data):
    # 현재 시간을 기반으로 파일명 생성
    timestamp = datetime.datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
    file_path = os.path.join(REQUEST_FOLDER, f'{timestamp}.bin')

    # 이진 데이터 저장
    with open(file_path, 'wb') as f:
        f.write(data)
    print(f'Request data saved as {file_path}')


def save_image_data(image_data, filename):
    # 이미지 파일 저장 경로 설정
    image_path = os.path.join(IMAGE_FOLDER, filename)

    # 이미지 데이터 저장
    with open(image_path, 'wb') as f:
        f.write(image_data)
    print(f'Image saved as {image_path}')


def handle_client(client_socket):
    try:
        # 모든 데이터를 받을 때까지 반복
        data = b""
        while True:
            packet = client_socket.recv(1024 * 1024)  # 최대 1MB 수신
            if not packet:  # 더 이상 수신할 데이터가 없으면 루프 종료
                break
            data += packet  # 받은 패킷을 기존 데이터에 추가

        if not data:
            print('No data received')
            return

        # 이진 데이터를 파일로 저장
        save_request_data(data)

        # 간단한 HTTP 200 응답 전송
        http_response = b"HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello, Client!"
        client_socket.sendall(http_response)

        # 간단한 멀티파트 파싱
        parts = data.split(b'\r\n')
        for part in parts:
            if b'Content-Disposition' in part and b'filename=' in part:
                # 파일 이름 추출
                start = part.find(b'filename="') + len(b'filename="')
                end = part.find(b'"', start)
                filename = part[start:end].decode()

                # 이미지 데이터 추출 (이 예제에서는 간단하게 처리)
                image_data_start = data.find(b'\r\n\r\n') + 4
                image_data_end = data.rfind(b'\r\n--')
                image_data = data[image_data_start:image_data_end]

                # 이미지 데이터 저장
                save_image_data(image_data, filename)
                break
    finally:
        client_socket.close()


def start_server():
    # 소켓 객체 생성
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((HOST, PORT))
        server_socket.listen(1)  # 연결 대기

        print(f'Server started on {HOST}:{PORT}')
        while True:
            # 클라이언트 연결 수락
            client_socket, addr = server_socket.accept()
            print(f'Accepted connection from {addr}')

            # 클라이언트 요청 처리
            handle_client(client_socket)


if __name__ == '__main__':
    start_server()
