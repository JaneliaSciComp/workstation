#version 410

layout (location=0) in vec3 iv;

uniform mat4 mvp3d;
uniform mat4 mvp2d;
uniform int pickId;
uniform int twoDimensional;

flat out int fragmentPickId;

void main()
{
  vec4 vp = vec4(iv.x, iv.y, iv.z, 1.0);
  if (twoDimensional==1) {
  //     gl_Position=vec4(iv.x, iv.y, 0.0, 1.0);
     vp.z=0.0;
     gl_Position = mvp2d * vp;
  } else {
     gl_Position = mvp3d * vp;
  }
  fragmentPickId=pickId;
}
