package ius;

import ius.Util.FFF;

import java.util.HashMap;

import com.example.opengles20.myGLRenderer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/* FontManager Class
 * Function : 폰트를 출력하기 위해 관리하는 클래스
 */
public class FontManager {

	int mTextureHandle; // ASCII와 Unicode 정보를 하나의 Texture에 작성한 TextureID를 저장한 변수
	FontObejct fontObject;
	Context mContext;

	private static FontManager instance;

	public static FontManager getInstance(){
		if(instance == null)
			instance = new FontManager();
		return instance;
	}

	/*
	 * TableTexture Class Function : Texture정보를 효율적으로 관리하기 위해 작성된 클래스 하나의 글자를
	 * 저장하게 됨
	 */
	public class TableTexture {
		public float top, left, bottom, right, width;
		public int type;

		public TableTexture(float x, float y, float pwidth, float pheight,
				int ptype) {
			top = y / (float) textureSize;
			left = x / (float) textureSize;
			bottom = (y + pheight) / (float) textureSize;
			right = (x + pwidth) / (float) textureSize;
			width = pwidth;
			type = ptype;
		}
	}

	private HashMap<Integer, TableTexture> FontsizeTable;
	private int textureSize;
	private float charHeight;
	// 한글 Unicode는 11,000개를 가짐
	// euc-kr은 2350개를 가짐
	private int KOREAN_LINE_LENGTH;
	// ASCII는 90개 정도를 가짐
	private final int ASCII_START = 32;
	private final int ASCII_END = 126;
	private final int FONTSIZE = 32;
	// 지원하지 않는 글자는 특수문자 'ㅁ'로 표현함
	private final int DEFAULT = 9633;

	Typeface mTF;

	@SuppressLint("UseSparseArrays")
	private FontManager() {

	}

	public void FontLoad(String fontName, Context context) {
		// mTF는 사용자의 *.fft파일을 읽고 적용함
		mTF = Typeface.createFromAsset(context.getAssets(), fontName);
		FontsizeTable = new HashMap<Integer, TableTexture>();
		mContext = context;
		
		// 한글 2350개만 사용함
		String hangle = getHangleFont();
		KOREAN_LINE_LENGTH = (int) Math.sqrt((hangle.length() + ASCII_END
				- ASCII_START + 2) * 0.66) + 1;

		float fontHeight;
		float fontDescent;

		// 속도 향상을 위해 RGBA의 3가지 색상을 적용한 FontMap을 만들음
		// 이 부분은 Shader와 관련이 있으므로 Shader도 수정이 필요함
		Paint textPaint = new Paint();
		textPaint.setTextSize(FONTSIZE);// default size
		textPaint.setAntiAlias(true);
		textPaint.setTypeface(mTF);// font

		Paint.FontMetrics fm = textPaint.getFontMetrics();
		fontHeight = (float) Math.ceil(Math.abs(fm.bottom) + Math.abs(fm.top));
		fontDescent = (float) Math.ceil(Math.abs(fm.descent));

		float[] charWidths = new float[hangle.length() + ASCII_END
				- ASCII_START + 2];
		float charWidthMax;
		charWidthMax = charHeight = 0;
		char[] s = new char[2];
		float[] w = new float[2];
		int cnt = 0;

		for (int i = 0; i < hangle.length(); i++) {
			s[0] = hangle.charAt(i);
			textPaint.getTextWidths(s, 0, 1, w);
			charWidths[cnt] = w[0];
			if (charWidths[cnt] > charWidthMax)
				charWidthMax = charWidths[cnt];
			++cnt;
		}
		for (int i = ASCII_START; i <= ASCII_END; i++) {
			s[0] = (char) i;
			textPaint.getTextWidths(s, 0, 1, w);
			// if(w[0] < 5f)w[0] = 5f;//5f is default size
			charWidths[cnt] = w[0];
			// Log.d("font","c:"+s[0]+"/w:"+charWidths[cnt]);
			if (charWidths[cnt] > charWidthMax)
				charWidthMax = charWidths[cnt];
			++cnt;
		}
		s[0] = (char) DEFAULT;
		textPaint.getTextWidths(s, 0, 1, w);
		charWidths[cnt] = w[0];
		if (charWidths[cnt] > charWidthMax)
			charWidthMax = charWidths[cnt];
		++cnt;

		charHeight = fontHeight;
		int maxSize = (int) (charWidthMax > charHeight ? charWidthMax
				: charHeight); // Log.d("font","maxSize :"+maxSize);
		textureSize = KOREAN_LINE_LENGTH * maxSize;
		Log.d("font", "bitmap SIZE :" + textureSize + "/" + textureSize);

		Bitmap bitmap = Bitmap.createBitmap(textureSize, textureSize,
				Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		bitmap.eraseColor(0);

		float x = 0f, y = charHeight - fontDescent;
		final float MARGIN = y;
		int TYPE = 0;
		for (int i = 0; i < hangle.length(); i++) {
			char C = hangle.charAt(i);
			s[0] = C;

			SetPaintColor(textPaint, TYPE);

			canvas.drawText(s, 0, 1, x, y, textPaint);
			FontsizeTable.put((int) C, new TableTexture(x, y - MARGIN,
					charWidths[i], charHeight, TYPE));

			if (TYPE == 2)
				x += (float) maxSize;
			TYPE = ++TYPE % 3;

			if ((int) x + maxSize > textureSize) {
				x = 0f;
				y += (float) maxSize;
			}
		}
		for (int i = ASCII_START; i <= ASCII_END; i++) {
			s[0] = (char) i;

			SetPaintColor(textPaint, TYPE);

			canvas.drawText(s, 0, 1, x, y, textPaint);
			FontsizeTable.put(i, new TableTexture(x, y - MARGIN,
					charWidths[hangle.length() + i - ASCII_START], charHeight,
					TYPE));

			if (TYPE == 2)
				x += (float) maxSize;
			TYPE = ++TYPE % 3;

			if ((int) x + maxSize > textureSize) {
				x = 0f;
				y += (float) maxSize;
			}
		}
		s[0] = (char) DEFAULT;

		SetPaintColor(textPaint, TYPE);

		canvas.drawText(s, 0, 1, x, y, textPaint);
		FontsizeTable.put(DEFAULT, new TableTexture(x, y - MARGIN,
				charWidths[hangle.length() + (ASCII_END - ASCII_START + 1)],
				charHeight, TYPE));

		final int[] textureHandle = new int[1];
		GLES20.glGenTextures(1, textureHandle, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		// TODO 최적화떄문에 수정함!
		// GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
		// GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
		// GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
		// GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		bitmap.recycle();

		mTextureHandle = textureHandle[0];
		fontObject = new FontObejct(myGLRenderer.mFontProgramHandle,
				mTextureHandle);
	}

	public void FontBegin() {
		GLES20.glUseProgram(myGLRenderer.mFontProgramHandle);
	}

	public void FontEnd() {
		GLES20.glUseProgram(myGLRenderer.mObjectProgramHandle);
	}

	/*
	 * 글자를 출력하는 함수 출력 방법으로 Text를 한 글자씩 출력함
	 */
	public void draw(String text, float x, float y, boolean CenterAlign,
			int color, float scale) {

		float indexXspace;
		if (CenterAlign == true) {
			indexXspace = x - getTextSize(text, scale);// center
			y -= FONTSIZE * 0.5f * scale;
		} else
			indexXspace = x;

		int len = text.length();
		for (int i = 0; i < len; i++) {
			char ch = text.charAt(i);
			TableTexture temp = FontsizeTable.get((int) (ch));
			if (temp == null)// don't have key
				temp = FontsizeTable.get(DEFAULT);

			fontObject.SetFontObject(indexXspace, y, color, temp, scale);
			fontObject.Draw();

			indexXspace += (temp.width) * scale;// *myGLRenderer.ssu;
		}
	}

	/* Text 정렬을 위해 전체 Text 크기를 얻는 함수 */
	public float getTextSize(String text, float scale) {
		int len = text.length();
		int textSize = 0;
		for (int i = 0; i < len; i++) {
			char ch = text.charAt(i);
			TableTexture temp = FontsizeTable.get((int) (ch));
			if (temp == null)// don't have key
				temp = FontsizeTable.get(DEFAULT);
			textSize += temp.width;
		}
		return (float) textSize * scale * 0.5f;
	}

	/*
	 * FontObject Class Function : Font 출력을 위한 클래스
	 */
	public class FontObejct extends iusObject {
		FFF fColor; // 색상 정보를 저장하는 변수
		TableTexture temp; // Texture 좌표 정보를 저장하는 변수

		protected FontObejct(int mProgramHandle, int pTextureHandle) {
			super(mProgramHandle);
			mTextureDataHandle = pTextureHandle;
		}

		public void SetFontObject(float px, float py, int pColor,
				TableTexture ptemp, float pscale) {
			x = px;
			y = py;
			fColor = new FFF(pColor);
			temp = ptemp;
			scale = pscale;
			mType = ptemp.type;
		}

		@Override
		public void Draw() {
			float nW = temp.width / 2 * myGLRenderer.ssx, nH = charHeight / 2
					* myGLRenderer.ssy;
			// Font는 Left, Top 기준으로 그려짐 < iusDraw의 첫번째 Parameter가 1f >
			iusDraw(1f, nW, nH, temp.left, temp.top, temp.right, temp.bottom,
					fColor.R, fColor.G, fColor.B);
		}
	}

	/* RGBA에서 TYPE에 맞는 결과를 세팅하는 함수 */
	private void SetPaintColor(Paint textPaint, int TYPE) {
		switch (TYPE) {
		case 0:
			textPaint.setARGB(0x7f, 0xff, 0x00, 0x00); // red
			break;
		case 1:
			textPaint.setARGB(0x7f, 0x00, 0xff, 0x00); // green
			break;
		case 2:
			textPaint.setARGB(0x7f, 0x00, 0x00, 0xff); // blue
			break;
		}
	}

	/* 한글 포트 정보를 읽는 함수 */
	private String getHangleFont() {
		String s = "가각간갇갈갉갊감갑값갓갔강갖갗같갚갛개객갠갤갬갭갯갰갱갸갹갼걀걋걍걔걘걜거걱건걷걸걺검겁것겄겅겆겉겊겋게겐겔겜겝겟겠겡겨격겪견겯결겸겹겻겼경곁계곈곌곕곗고곡곤곧골곪곬곯곰곱곳공곶과곽관괄괆괌괍괏광괘괜괠괩괬괭괴괵괸괼굄굅굇굉교굔굘굡굣구국군굳굴굵굶굻굼굽굿궁궂궈궉권궐궜궝궤궷";
		s += "귀귁귄귈귐귑귓규균귤그극근귿글긁금급긋긍긔기긱긴긷길긺김깁깃깅깆깊까깍깎깐깔깖깜깝깟깠깡깥깨깩깬깰깸깹깻깼깽꺄꺅꺌꺼꺽꺾껀껄껌껍껏껐껑께껙껜껨껫껭껴껸껼꼇꼈꼍꼐꼬꼭꼰꼲꼴꼼꼽꼿꽁꽂꽃꽈꽉꽐꽜꽝꽤꽥꽹꾀꾄꾈꾐꾑꾕꾜꾸꾹꾼꿀꿇꿈꿉꿋꿍꿎꿔꿜꿨꿩꿰꿱꿴꿸뀀뀁뀄뀌뀐뀔뀜뀝뀨끄끅끈끊끌";
		s += "끎끓끔끕끗끙끝끼끽낀낄낌낍낏낑나낙낚난낟날낡낢남납낫났낭낮낯낱낳내낵낸낼냄냅냇냈냉냐냑냔냘냠냥너넉넋넌널넒넓넘넙넛넜넝넣네넥넨넬넴넵넷넸넹녀녁년녈념녑녔녕녘녜녠노녹논놀놂놈놉놋농높놓놔놘놜놨뇌뇐뇔뇜뇝뇟뇨뇩뇬뇰뇹뇻뇽누눅눈눋눌눔눕눗눙눠눴눼뉘뉜뉠뉨뉩뉴뉵뉼늄늅늉느늑는늘늙늚늠";
		s += "늡늣능늦늪늬늰늴니닉닌닐닒님닙닛닝닢다닥닦단닫달닭닮닯닳담답닷닸당닺닻닿대댁댄댈댐댑댓댔댕댜더덕덖던덛덜덞덟덤덥덧덩덫덮데덱덴델뎀뎁뎃뎄뎅뎌뎐뎔뎠뎡뎨뎬도독돈돋돌돎돐돔돕돗동돛돝돠돤돨돼됐되된될됨됩됫됴두둑둔둘둠둡둣둥둬뒀뒈뒝뒤뒨뒬뒵뒷뒹듀듄듈듐듕드득든듣들듦듬듭듯등듸디딕딘";
		s += "딛딜딤딥딧딨딩딪따딱딴딸땀땁땃땄땅땋때땍땐땔땜땝땟땠땡떠떡떤떨떪떫떰떱떳떴떵떻떼떽뗀뗄뗌뗍뗏뗐뗑뗘뗬또똑똔똘똥똬똴뙈뙤뙨뚜뚝뚠뚤뚫뚬뚱뛔뛰뛴뛸뜀뜁뜅뜨뜩뜬뜯뜰뜸뜹뜻띄띈띌띔띕띠띤띨띰띱띳띵라락란랄람랍랏랐랑랒랖랗래랙랜랠램랩랫랬랭랴략랸럇량러럭런럴럼럽럿렀렁렇레렉렌렐렘렙렛렝";
		s += "려력련렬렴렵렷렸령례롄롑롓로록론롤롬롭롯롱롸롼뢍뢨뢰뢴뢸룀룁룃룅료룐룔룝룟룡루룩룬룰룸룹룻룽뤄뤘뤠뤼뤽륀륄륌륏륑류륙륜률륨륩륫륭르륵른를름릅릇릉릊릍릎리릭린릴림립릿링마막만많맏말맑맒맘맙맛망맞맡맣매맥맨맬맴맵맷맸맹맺먀먁먈먕머먹먼멀멂멈멉멋멍멎멓메멕멘멜멤멥멧멨멩며멱면멸몃몄";
		s += "명몇몌모목몫몬몰몲몸몹못몽뫄뫈뫘뫙뫼묀묄묍묏묑묘묜묠묩묫무묵묶문묻물묽묾뭄뭅뭇뭉뭍뭏뭐뭔뭘뭡뭣뭬뮈뮌뮐뮤뮨뮬뮴뮷므믄믈믐믓미믹민믿밀밂밈밉밋밌밍및밑바박밖밗반받발밝밞밟밤밥밧방밭배백밴밸뱀뱁뱃뱄뱅뱉뱌뱍뱐뱝버벅번벋벌벎범법벗벙벚베벡벤벧벨벰벱벳벴벵벼벽변별볍볏볐병볕볘볜보복볶";
		s += "본볼봄봅봇봉봐봔봤봬뵀뵈뵉뵌뵐뵘뵙뵤뵨부북분붇불붉붊붐붑붓붕붙붚붜붤붰붸뷔뷕뷘뷜뷩뷰뷴뷸븀븃븅브븍븐블븜븝븟비빅빈빌빎빔빕빗빙빚빛빠빡빤빨빪빰빱빳빴빵빻빼빽뺀뺄뺌뺍뺏뺐뺑뺘뺙뺨뻐뻑뻔뻗뻘뻠뻣뻤뻥뻬뼁뼈뼉뼘뼙뼛뼜뼝뽀뽁뽄뽈뽐뽑뽕뾔뾰뿅뿌뿍뿐뿔뿜뿟뿡쀼쁑쁘쁜쁠쁨쁩삐삑삔삘삠삡삣삥";
		s += "사삭삯산삳살삵삶삼삽삿샀상샅새색샌샐샘샙샛샜생샤샥샨샬샴샵샷샹섀섄섈섐섕서석섞섟선섣설섦섧섬섭섯섰성섶세섹센셀셈셉셋셌셍셔셕션셜셤셥셧셨셩셰셴셸솅소속솎손솔솖솜솝솟송솥솨솩솬솰솽쇄쇈쇌쇔쇗쇘쇠쇤쇨쇰쇱쇳쇼쇽숀숄숌숍숏숑수숙순숟술숨숩숫숭숯숱숲숴쉈쉐쉑쉔쉘쉠쉥쉬쉭쉰쉴쉼쉽쉿슁슈";
		s += "슉슐슘슛슝스슥슨슬슭슴습슷승시식신싣실싫심십싯싱싶싸싹싻싼쌀쌈쌉쌌쌍쌓쌔쌕쌘쌜쌤쌥쌨쌩썅써썩썬썰썲썸썹썼썽쎄쎈쎌쏀쏘쏙쏜쏟쏠쏢쏨쏩쏭쏴쏵쏸쐈쐐쐤쐬쐰쐴쐼쐽쑈쑤쑥쑨쑬쑴쑵쑹쒀쒔쒜쒸쒼쓩쓰쓱쓴쓸쓺쓿씀씁씌씐씔씜씨씩씬씰씸씹씻씽아악안앉않알앍앎앓암압앗았앙앝앞애액앤앨앰앱앳앴앵야약";
		s += "얀얄얇얌얍얏양얕얗얘얜얠얩어억언얹얻얼얽얾엄업없엇었엉엊엌엎에엑엔엘엠엡엣엥여역엮연열엶엷염엽엾엿였영옅옆옇예옌옐옘옙옛옜오옥온올옭옮옰옳옴옵옷옹옻와왁완왈왐왑왓왔왕왜왝왠왬왯왱외왹왼욀욈욉욋욍요욕욘욜욤욥욧용우욱운울욹욺움웁웃웅워웍원월웜웝웠웡웨웩웬웰웸웹웽위윅윈윌윔윕윗윙";
		s += "유육윤율윰윱윳융윷으윽은을읊음읍읏응읒읓읔읕읖읗의읜읠읨읫이익인일읽읾잃임입잇있잉잊잎자작잔잖잗잘잚잠잡잣잤장잦재잭잰잴잼잽잿쟀쟁쟈쟉쟌쟎쟐쟘쟝쟤쟨쟬저적전절젊점접젓정젖제젝젠젤젬젭젯젱져젼졀졈졉졌졍졔조족존졸졺좀좁좃종좆좇좋좌좍좔좝좟좡좨좼좽죄죈죌죔죕죗죙죠죡죤죵주죽준줄줅";
		s += "줆줌줍줏중줘줬줴쥐쥑쥔쥘쥠쥡쥣쥬쥰쥴쥼즈즉즌즐즘즙즛증지직진짇질짊짐집짓징짖짙짚짜짝짠짢짤짧짬짭짯짰짱째짹짼쨀쨈쨉쨋쨌쨍쨔쨘쨩쩌쩍쩐쩔쩜쩝쩟쩠쩡쩨쩽쪄쪘쪼쪽쫀쫄쫌쫍쫏쫑쫓쫘쫙쫠쫬쫴쬈쬐쬔쬘쬠쬡쭁쭈쭉쭌쭐쭘쭙쭝쭤쭸쭹쮜쮸쯔쯤쯧쯩찌찍찐찔찜찝찡찢찧차착찬찮찰참찹찻찼창찾채책챈챌챔";
		s += "챕챗챘챙챠챤챦챨챰챵처척천철첨첩첫첬청체첵첸첼쳄쳅쳇쳉쳐쳔쳤쳬쳰촁초촉촌촐촘촙촛총촤촨촬촹최쵠쵤쵬쵭쵯쵱쵸춈추축춘출춤춥춧충춰췄췌췐취췬췰췸췹췻췽츄츈츌츔츙츠측츤츨츰츱츳층치칙친칟칠칡침칩칫칭카칵칸칼캄캅캇캉캐캑캔캘캠캡캣캤캥캬캭컁커컥컨컫컬컴컵컷컸컹케켁켄켈켐켑켓켕켜켠켤켬";
		s += "켭켯켰켱켸코콕콘콜콤콥콧콩콰콱콴콸쾀쾅쾌쾡쾨쾰쿄쿠쿡쿤쿨쿰쿱쿳쿵쿼퀀퀄퀑퀘퀭퀴퀵퀸퀼큄큅큇큉큐큔큘큠크큭큰클큼큽킁키킥킨킬킴킵킷킹타탁탄탈탉탐탑탓탔탕태택탠탤탬탭탯탰탱탸턍터턱턴털턺텀텁텃텄텅테텍텐텔템텝텟텡텨텬텼톄톈토톡톤톨톰톱톳통톺톼퇀퇘퇴퇸툇툉툐투툭툰툴툼툽툿퉁퉈퉜퉤튀";
		s += "튁튄튈튐튑튕튜튠튤튬튱트특튼튿틀틂틈틉틋틔틘틜틤틥티틱틴틸팀팁팃팅파팍팎판팔팖팜팝팟팠팡팥패팩팬팰팸팹팻팼팽퍄퍅퍼퍽펀펄펌펍펏펐펑페펙펜펠펨펩펫펭펴편펼폄폅폈평폐폘폡폣포폭폰폴폼폽폿퐁퐈퐝푀푄표푠푤푭푯푸푹푼푿풀풂품풉풋풍풔풩퓌퓐퓔퓜퓟퓨퓬퓰퓸퓻퓽프픈플픔픕픗피픽핀필핌핍핏핑";
		s += "하학한할핥함합핫항해핵핸핼햄햅햇했행햐향허헉헌헐헒험헙헛헝헤헥헨헬헴헵헷헹혀혁현혈혐협혓혔형혜혠혤혭호혹혼홀홅홈홉홋홍홑화확환활홧황홰홱홴횃횅회획횐횔횝횟횡효횬횰횹횻후훅훈훌훑훔훗훙훠훤훨훰훵훼훽휀휄휑휘휙휜휠휨휩휫휭휴휵휸휼흄흇흉흐흑흔흖흗흘흙흠흡흣흥흩희흰흴흼흽힁히힉힌힐힘힙힛힝";
		return s;
	}
}
