
/*void clear(__global __write_only int* image, int size, int color) 
{
    for (int i = 0; i < size; ++i) 
    {
        image[i] = color;
    }
}*/


int add_сolor(int src, int dst, float power) 
{
    if (power <= 0.05f) 
    {
        return src;
    } 
    else 
        if (power >= 0.95f) 
        {
            return dst;
        } 
        else 
        {
            float ps = 1.0f - power;
            float pd = power;

            int rs = (int)(((src >> 16) & 0xFF) * ps);
            int gs = (int)(((src >>  8) & 0xFF) * ps);
            int bs = (int)(((src      ) & 0xFF) * ps);

            int rd = (int)(((dst >> 16) & 0xFF) * pd);
            int gd = (int)(((dst >>  8) & 0xFF) * pd);
            int bd = (int)(((dst      ) & 0xFF) * pd);

            int r = (rs + rd);// % 255;
            int g = (gs + gd);// % 255;
            int b = (bs + bd);// % 255;

            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
}

void draw_vertical_line(__global __write_only int* image, int x, int screen_width, int screen_height, int ytop, int ybottom, int color) 
{
    if (ytop > ybottom || ytop + ybottom <= 0) 
    {
        return;
    }
/*
    x = max(x, 0);
    x = min(x, screen_width - 1);

    ytop = max(ytop, 0);
    ytop = min(ytop, screen_height - 1);

    ytop = max(ybottom, 0);
    ytop = min(ybottom, screen_height - 1);
*/

    x = x < 0 ? 0 : x;
    x = x >= screen_width ? screen_width - 1 : x;

    ytop = ytop < 0 ? 0 : ytop;
    ytop = ytop >= screen_height ? screen_height - 1 : ytop;

    ybottom = ybottom < 0 ? 0 : ybottom;
    ybottom = ybottom >= screen_height ? screen_height - 1 : ybottom;

    int offset = ytop * screen_width + x;
    for (int y = ytop; y < ybottom; ++y, offset += screen_width) 
    {
        image[offset] = color;
    }
}

// __read_only int* color_map
// __read_only int* height_map
// __write_only int* screen
// __read_only float* camera_params 
// __read_only int* global_params [ int map_width, int map_height, int screen_height, int screen_width, ... ]
__kernel void render(__global __read_only  int*   color_map,
                     __global __read_only  int*   height_map,
                     __global __write_only int*   screen,
                     __global __read_only  float* camera_params,
                     __global __read_only  int*   global_params)
{
    int n = 640;
    int gid = get_global_id(0);
    if (gid >= n)
    {
        return;
    }

    // global options
    int map_width  = global_params[0];
    int map_height = global_params[1];
    int screen_width  = global_params[2];
    int screen_height = global_params[3];
    int map_width_period  = map_width - 1;
    int map_height_period = map_height - 1;
    
    bool  fog_enabled  = global_params[4];
    int   fog_color    = global_params[5];
    float fog_start    = global_params[6];
    float fog_end      = global_params[7];
    float fog_factor   = 1.0f / (fog_end - fog_start);
    int   sky_color    = global_params[8];
    float detalization = global_params[9] * 0.0001f;

    // camera options   
    float sx, sy;
    float cx = camera_params[0],
          cy = camera_params[1];
    float tx = camera_params[2],
          ty = camera_params[3];
    float camera_angle = camera_params[4];
    float distance = camera_params[5];
    float fov      = camera_params[6];
    float half_fov = fov * 0.5f;
    float camera_height = camera_params[7];
    float horizon  = camera_params[8];

    // calculate part of screen 
    int xStep = screen_width / n;
    int xStart = 0;
    float angleStep = fov / n;
    float angleStart = -half_fov;
        
    int xFrom, xTo;
    float angleFrom, angleTo;
    /*for (int i = 0; i <= gid; ++i, xStart += xStep, angleStart += angleStep) 
    {
        xFrom = xStart;
        xTo   = xFrom + xStep;   
        angleFrom = angleStart;
        angleTo   = angleStart + angleStep;
        if (i == n - 1) 
        {
            xTo = screen_width;
            angleTo = half_fov;
        }
    }*/
    xFrom = xStart + gid * xStep;
    angleFrom = angleStart + gid * angleStep;
    if (gid < n - 1) 
    {
        xTo   = xFrom + xStep;  
        angleTo   = angleStart + angleStep;
    } 
    else 
    {
        xTo = screen_width;
        angleTo = half_fov;
    }

    // prepare...
    float angle_from = angleFrom;//-half_fov;
    float angle_ray  = camera_angle + angle_from;
    float correct_angle = angle_from;
    float angle_step = fov / screen_width;
    
    int sky_height;
    float power;

    // render!!!
    //for (int x = 0; x < screen_width; ++x, angle_ray += angle_step, correct_angle += angle_step) 
    for (int x = xFrom; x < xTo; ++x, angle_ray += angle_step, correct_angle += angle_step)     
    {  
        float rad = radians(angle_ray);
        sx = native_sin(rad);  
        sy = native_cos(rad);

        // correct length of ray
        float correct = 1.0f / native_cos(radians(correct_angle));
        sx *= correct;
        sy *= correct;

        sky_height = screen_height;

        for (float dz = 0.1f, z = 1.0f; z < distance; z += dz, dz += detalization) {
            int px = (int)(cx + sx * z),
                py = (int)(cy + sy * z);

            while (px < 0) { px += map_width;  }
            while (py < 0) { py += map_height; }
            if (px >= map_width ) { px %= map_width_period;  }
            if (py >= map_height) { py %= map_height_period; }

            int pos = px + py * map_width;

            int height_on_screen = (int)((camera_height - height_map[pos]) * (screen_height >> 1) / z + horizon);
            if (height_on_screen < 0) 
            {
                height_on_screen = -height_on_screen;
            }

            int color = color_map[pos];

            /*if (fog_enabled) {
                if (z >= fog_start) {
                    if (z < fog_end) {
                        power = (z - fog_start) * fog_factor;
                        color = add_color(color, fog_color, power);
                    } else {
                        color = fog_color;
                    }
                }
            }*/

            draw_vertical_line(screen, x, screen_width, screen_height, height_on_screen, sky_height, color);
            
            if (height_on_screen < sky_height) {
                sky_height = height_on_screen;
            }

            // all height filled - stop ray
            if (sky_height == 0) {
                break;
            }
        }

        // draw sky
        draw_vertical_line(screen, x, screen_width, screen_height, 0, sky_height, sky_color);
    }
}
