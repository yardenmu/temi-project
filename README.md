# 🤖 Temi Medical Assistant – Kotlin Project

An interactive medical assistant system built for the **Temi robot**, designed to guide patients through a structured scenario of **heating rice**. The system supports real-time communication between the robot and the backend, tracking user progress and providing adaptive responses.

---

## 📱 Technologies Used

- **Kotlin** – Android client development  
- **Temi SDK** – Robot control, navigation, display management, and voice interaction  
- **WebSocket** – Real-time bi-directional communication between Temi and the backend server  
- **Backend Server** – Scenario management, decision-making logic, and image analysis (e.g., YOLO integration)

---

## 🎯 Project Goals

- Guide the user step-by-step through a **rice heating scenario**
- Provide **voice and visual instructions** using Temi
- Communicate with a backend server to:
  - Receive and execute each step in real time
  - Validate step completion using visual and sensor input
- Adapt the flow based on user performance, including **reminders, retries, and scoring**

---

## 🧠 Key Features

- Real-time **robot-server communication** using WebSocket
- Execution of predefined scenario steps with:
  - Object detection (e.g., microwave, rice container)
  - Temperature and force sensor checks
  - Timeout handling and user feedback
- Modular and maintainable Kotlin architecture:
  - **ScenarioManager** – controls scenario flow
  - **ScenarioStep** – defines actions and conditions
  - **RobotActionScript** – handles robot behavior
  - **StepCondition** – validates that the user performed the step

---

## 📂 Project Structure (Client-side)
```
📁 temi-app/
┣ 📁 helpers/
┣ 📁 models/
┣ 📁 repository/
┣ 📁 viewmodel/
┗ 📄 MainActivity.kt
```
## 👥 Authors

This project was developed collaboratively by:

- Yarden Mugany  
- Aviv hanoon 
- Yonatan Amsalem  
- Idan Vahab 
