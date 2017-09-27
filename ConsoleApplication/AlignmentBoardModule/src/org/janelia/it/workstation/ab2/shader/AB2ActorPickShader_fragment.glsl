#version 410

in int fragmentPickId;
out int fragmentPickId2;

void main() {
    fragmentPickId2=fragmentPickId;
}

