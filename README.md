# UmoCam

UmoCam is an experimental open-source Android application that tracks a selected color through the device camera and visualizes its movement as a continuous drawing on a canvas in real time.

The core idea behind the project is **digitizing physical drawing**:
by attaching a small piece of colored paper to the tip of a real pencil, the app tracks that color and converts the pencil’s movement into digital strokes on the screen.

## ✨ Concept

Instead of using a stylus or touch input, UmoCam allows users to draw **in the air or on paper**, while the phone camera observes a physical color marker.

This makes it possible to:

* Capture real hand movements
* Transform analog drawing into digital form
* Experiment with computer vision–based input methods

## 🎯 Features

* 📷 **Real-time camera feed**
* 🎨 **Color selection via camera**

  * Choose a target color directly from the live image
* 🔍 **Color tracking**

  * Detects and follows an object of the selected color
* ✏️ **Trajectory visualization**

  * Draws the movement path of the color marker on a canvas
* 🖌️ **Physical-to-digital drawing**

  * Track a colored paper attached to a real pencil
* ⚡ Real-time processing optimized for mobile devices

## 🧠 How It Works

1. The camera captures a live video stream.
2. The user selects a target color directly from the camera preview.
3. The app continuously detects pixels matching that color.
4. The centroid of the detected color region is calculated.
5. The movement path of this point is drawn onto a canvas overlay.

In practice, this allows the phone to “see” the tip of a pencil and draw digitally based on its motion.

## 🧪 Usage Tips

* Use a **bright, saturated color** (red, green, blue, etc.)
* Avoid backgrounds with similar colors
* Ensure good lighting conditions
* A small piece of colored paper on the pencil tip works best

## 🛠️ Possible Improvements

* Noise filtering and smoothing of trajectories
* Multi-color tracking
* Stroke thickness control
* Saving drawings as images or vector paths
* Recording drawing sessions as video
* OpenCV-based optimizations

## 📄 License

This project is licensed under the **MIT License**.
See the [LICENSE](LICENSE) file for more information.
