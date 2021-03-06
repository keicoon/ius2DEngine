package ius;

/* PrintTextObject Class
 * Function : 대화하는 효과의 Text를 출력하는 클래스 
 * */
public class PrintTextObject {
	private FontManager FM;
	
	private int text_INDEX = 0;		// 현재까지 출력된 text의 위치
	private String myText;			// 출력해야 하는 Text의 값
	private float x, y;			// 출력해야 하는 Text의 위치
	private int Color;				// 출력해야 하는 Text의 색상
	
	private boolean STOP = false;	// Text의 현재 출력상태
	private float stopTIME;			// 대기해야 하는 시간
	private float TIME;				// 현재 시간
	
	public PrintTextObject(FontManager pFM, String pText,
			float pX, float pY,
			int pColor){
		FM = pFM;
		x = pX; y = pY; Color = pColor;
		myText = pText;
		stopTIME = 0f; TIME = 0f;
	}
	/* 특정 Char에서는 Text가 지연되야 하고 그런 효과를 주기 위해 현재 text의 위치를 잡아주는 함수 */
	public boolean Update(float deltaTime, boolean KEY){
		TIME += deltaTime;
		// 특정 문자를 구별하고 지연시간을 설정함
		switch (myText.charAt(text_INDEX)) {
		case ' ':
			stopTIME = 0.25f;
			break;
		case '\n':
			stopTIME = 0.5f;
//			STOP = true;
			break;
		case '!':
		case '?':
		case '.':
			stopTIME = 0.25f;
			break;
		default:
			stopTIME = 0.1f;
			break;
		}
		// 현재 상태와 시간을 비교하여 상태를 업데이트함
		if (TIME > stopTIME && !STOP) {
			if(text_INDEX >= myText.length() - 1){
				STOP = true;
			}
			else{
				++text_INDEX;
				TIME = 0f;
			}
		}
		// 모든 Text가 출력이 완료됬을 때 분기를 업데이트함
		if (KEY && STOP) {
			STOP = false;
			if(text_INDEX < myText.length() - 1){
				++text_INDEX;
				STOP = false;
			}
			else{
				STOP = true;
				return true;
			}
		}
		return false;
	}
	/* FontManager Class를 사용해 Text를 출력함 */
	public void Draw(){
		String currentText = myText.substring(0, text_INDEX);
		String Text[] = currentText.split("\n");

		float vY = y;
		for(int i=0;i<Text.length;i++){
			FM.draw(Text[i], x, vY, false, Color, 1.5f);
			vY -= 80f;
		}
	}
	public void Draw(float px, float py, int pColor){
		String currentText = myText.substring(0, text_INDEX);
		String Text[] = currentText.split("\n");

		float vY = py;
		for(int i=0;i<Text.length;i++){
			FM.draw(Text[i], px, vY, false, pColor, 1.5f);
			vY -= 80f;
		}
	}
	public float getX(){return x;}
	public float getY(){return y;}
}
