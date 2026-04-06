# Jax Gamepad

A universal mobile controller for ROS-based robots using rosbridge.

Control your robot with dual joysticks, height / speed adjustment, and customizable behavior modes — all from a clean, real-time interface.

---

## 🚀 Features

- Dual joystick control (movement + body control)
- Height / posture slider
- Fully customizable mode buttons
- Auto-discovery of ROS topics
- Works over rosbridge (WebSocket)
- Robot-agnostic design (no code changes required)

---

## 📡 Requirements

Your robot must provide:

- A running rosbridge websocket server  
  (e.g. ws://<robot-ip>:9090)

- A motion topic using:
  geometry_msgs/Twist

- A mode/behavior topic using:
  std_msgs/String

---

## ⚙️ Setup

1. Launch rosbridge on your robot:
   ros2 run rosbridge_server rosbridge_websocket

2. Connect the app to your robot:
   ws://<robot-ip>:9090

3. Tap Scan to discover available topics

4. Select:
   - Motion Topic (Twist)
   - Mode Topic (String)

5. Configure your mode buttons

---

## 🎮 Controls

- Left Joystick → Forward / Strafe  
- Right Joystick → Turn or Body Control (mode dependent)  
- Vertical Slider → Height / posture  
- Mode Buttons → Switch behaviors  

---

## 🔘 Modes

Modes are fully customizable.

Each button has:
- Label → Displayed in the UI  
- Command → Sent to the robot  

Example:

Walk → trot  
Stand → stand  
Sit → sit  
Lay → lay  

The robot is responsible for interpreting these commands.

---

## 🔍 Topic Selection

The app does not require fixed topic names.

Examples of valid topics:

Motion:
- /cmd_vel
- /robot/cmd_vel
- /base_controller/cmd_vel

Mode:
- /robot_mode
- /behavior_mode
- /dingo_mode

Use the built-in Scan feature to select the correct ones.

---

## 🧠 How It Works

The app sends two types of commands:

Motion:
- /cmd_vel → geometry_msgs/Twist  
  Used for movement, turning, height, roll, and pitch

Mode:
- /robot_mode → std_msgs/String  
  Used to trigger behaviors like walk, sit, or rest

---

## 🧩 Optional Features

These are not required but improve the experience:

- /joint_states → smoother transitions, future telemetry  
- /odom → speed and position feedback  
- Video stream → live camera feed  

---

## 🧯 Troubleshooting

Robot connects but won’t move:
- Verify Motion Topic is correct  
- Ensure it uses geometry_msgs/Twist  
- Check robot is in a movement-enabled mode  

Modes don’t work:
- Verify Mode Topic is correct  
- Ensure it uses std_msgs/String  
- Confirm command strings match robot expectations  

Topics don’t appear in scan:
- Ensure rosbridge is running  
- Check network connection  
- Verify topics are actively published  

Movement is reversed or incorrect:
- Robot may use different axis conventions  
- Adjust in robot code or adapter layer  

Works in some modes but not others:
- Some modes intentionally disable motion  
- Confirm robot supports control in that mode  

---

## 🔧 Compatibility

- ROS2 (recommended)  
- ROS1 (via rosbridge)  
- Not compatible without rosbridge  

---

## 📌 Notes

- Topic names are fully configurable  
- Mode names are not enforced  
- No robot-side code changes are required if compatible topics exist  

---

## 📖 Help

For setup and usage help, open the Help / Docs section in the app.

---

## 🧠 Philosophy

This app is designed as a universal robot interface, not a robot-specific controller.

It separates:

App → high-level commands  
Robot → low-level control  

This allows the same app to work across many different robots.

---


