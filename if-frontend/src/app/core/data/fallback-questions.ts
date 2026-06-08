import { Question } from '../models/question.model';

/**
 * Banco local de preguntas por pilar.
 * Se usa INMEDIATAMENTE mientras el backend carga en segundo plano.
 * Garantiza carga instantánea sin depender del estado del servidor.
 */

const REC: Question[] = [
  { id: 9001, questionKey: 'FB_REC_001', text: '¿Con qué frecuencia los miembros de tu familia expresan sus emociones abiertamente?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9002, questionKey: 'FB_REC_002', text: 'Cuando alguien en tu familia está molesto, ¿los demás lo reconocen y responden con empatía?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9003, questionKey: 'FB_REC_003', text: '¿En tu hogar es seguro hablar de sentimientos como miedo, tristeza o frustración sin ser juzgado?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9004, questionKey: 'FB_REC_004', text: '¿Con qué frecuencia las tensiones en tu familia escalan a conflictos difíciles de resolver?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, reverseQuestion: true, severityWeight: 2 },
  { id: 9005, questionKey: 'FB_REC_005', text: '¿Los miembros de tu familia se escuchan activamente cuando alguien habla, sin interrumpir?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9006, questionKey: 'FB_REC_006', text: '¿Con qué frecuencia las conversaciones importantes en tu hogar terminan en malentendidos?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, reverseQuestion: true, severityWeight: 2 },
  { id: 9007, questionKey: 'FB_REC_007', text: '¿Cada miembro siente que puede dar su opinión sin que lo descarten o ridiculicen?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'M1', active: true, severityWeight: 2 },
  { id: 9008, questionKey: 'FB_REC_008', text: '¿Tu familia encuentra momentos regulares para conversar sobre cómo está cada uno?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'M1', active: true, severityWeight: 2 },
  { id: 9009, questionKey: 'FB_REC_009', text: '¿Existe una rutina de convivencia familiar que se respete consistentemente (comidas juntos, noches de familia)?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9010, questionKey: 'FB_REC_010', text: '¿Los roles y responsabilidades en el hogar están claramente definidos y son respetados por todos?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9011, questionKey: 'FB_REC_011', text: '¿Tu familia mantiene hábitos de cuidado físico y emocional compartidos (ejercicio, recreación, descanso)?', dimension: 'habitos', area: 'HABITOS', type: 'ADAPTIVE', pillarName: 'reconocimiento', milestoneCode: 'M1', active: true, severityWeight: 1 },
  { id: 9012, questionKey: 'FB_REC_012', text: '¿Con qué frecuencia la tecnología (celulares, TV) interrumpe los momentos de conexión familiar?', dimension: 'habitos', area: 'HABITOS', type: 'ADAPTIVE', pillarName: 'reconocimiento', milestoneCode: 'M1', active: true, reverseQuestion: true, severityWeight: 1 },
  { id: 9013, questionKey: 'FB_REC_013', text: '¿El tiempo que pasan juntos como familia es de calidad, con atención plena entre todos?', dimension: 'tiempos', area: 'TIEMPOS', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9014, questionKey: 'FB_REC_014', text: '¿Cada miembro siente que su tiempo individual es respetado y valorado por los demás?', dimension: 'tiempos', area: 'TIEMPOS', type: 'CORE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 2 },
  { id: 9015, questionKey: 'FB_REC_015', text: '¿Tu familia planifica con anticipación los momentos de convivencia importantes (vacaciones, celebraciones)?', dimension: 'tiempos', area: 'TIEMPOS', type: 'ADAPTIVE', pillarName: 'reconocimiento', milestoneCode: 'M1', active: true, severityWeight: 1 },
  { id: 9016, questionKey: 'FB_REC_016', text: '¿Hay en tu familia alguien que actúe como regulador emocional, ayudando a calmar situaciones difíciles?', dimension: 'emociones', area: 'EMOCIONES', type: 'ADAPTIVE', pillarName: 'reconocimiento', milestoneCode: 'W1', active: true, severityWeight: 1 },
  { id: 9017, questionKey: 'FB_REC_017', text: '¿Tu familia tiene formas establecidas de calmarse cuando hay una discusión fuerte?', dimension: 'emociones', area: 'EMOCIONES', type: 'FASE_PILLAR', pillarName: 'reconocimiento', milestoneCode: 'M1', active: true, severityWeight: 2 },
  { id: 9018, questionKey: 'FB_REC_018', text: '¿Se piden disculpas genuinamente en tu familia cuando alguien dice algo hiriente?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'FASE_PILLAR', pillarName: 'reconocimiento', milestoneCode: 'M2', active: true, severityWeight: 2 },
  { id: 9019, questionKey: 'FB_REC_019', text: '¿Las reglas del hogar se establecen mediante conversación o son impuestas por una sola persona?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'FASE_PILLAR', pillarName: 'reconocimiento', milestoneCode: 'M2', active: true, severityWeight: 2 },
  { id: 9020, questionKey: 'FB_REC_020', text: '¿Cuándo hay un conflicto, tu familia busca soluciones juntos en lugar de culparse mutuamente?', dimension: 'emociones', area: 'EMOCIONES', type: 'FASE_PILLAR', pillarName: 'reconocimiento', milestoneCode: 'M3', active: true, severityWeight: 2 },
];

const AMOR: Question[] = [
  { id: 9101, questionKey: 'FB_AMO_001', text: '¿Con qué frecuencia los miembros de tu familia se expresan afecto físico (abrazos, caricias, contacto)?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'amor', milestoneCode: 'M4', active: true, severityWeight: 2 },
  { id: 9102, questionKey: 'FB_AMO_002', text: '¿Sientes que el amor en tu familia se expresa de maneras que cada miembro puede recibir y comprender?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'amor', milestoneCode: 'M4', active: true, severityWeight: 2 },
  { id: 9103, questionKey: 'FB_AMO_003', text: '¿Los miembros de tu familia se dicen palabras de apreciación y reconocimiento regularmente?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'amor', milestoneCode: 'M4', active: true, severityWeight: 2 },
  { id: 9104, questionKey: 'FB_AMO_004', text: '¿Tu familia celebra los logros y avances de cada miembro, por pequeños que sean?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'amor', milestoneCode: 'M6', active: true, severityWeight: 2 },
  { id: 9105, questionKey: 'FB_AMO_005', text: '¿Existen rituales de amor en tu familia (buenos días, buenas noches, bendiciones)?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'amor', milestoneCode: 'M4', active: true, severityWeight: 2 },
  { id: 9106, questionKey: 'FB_AMO_006', text: '¿Tu familia crea momentos especiales de conexión emocional profunda con regularidad?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'amor', milestoneCode: 'M6', active: true, severityWeight: 2 },
  { id: 9107, questionKey: 'FB_AMO_007', text: '¿El tiempo de calidad familiar incluye actividades que todos disfrutan y eligen juntos?', dimension: 'tiempos', area: 'TIEMPOS', type: 'CORE', pillarName: 'amor', milestoneCode: 'M4', active: true, severityWeight: 2 },
  { id: 9108, questionKey: 'FB_AMO_008', text: '¿Cada miembro siente que recibe suficiente atención y tiempo de los demás?', dimension: 'tiempos', area: 'TIEMPOS', type: 'CORE', pillarName: 'amor', milestoneCode: 'M6', active: true, severityWeight: 2 },
  { id: 9109, questionKey: 'FB_AMO_009', text: '¿Tu familia comparte proyectos o metas comunes que fortalecen su unidad?', dimension: 'habitos', area: 'HABITOS', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M9', active: true, severityWeight: 1 },
  { id: 9110, questionKey: 'FB_AMO_010', text: '¿Cuándo un miembro está en crisis, los demás se movilizan para apoyarlo activamente?', dimension: 'emociones', area: 'EMOCIONES', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M9', active: true, severityWeight: 2 },
  { id: 9111, questionKey: 'FB_AMO_011', text: '¿En tu familia se reconocen y valoran las diferencias individuales de cada miembro?', dimension: 'emociones', area: 'EMOCIONES', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M6', active: true, severityWeight: 1 },
  { id: 9112, questionKey: 'FB_AMO_012', text: '¿Existe en tu hogar un ambiente de seguridad donde nadie teme ser juzgado por quien es?', dimension: 'emociones', area: 'EMOCIONES', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M4', active: true, severityWeight: 2 },
  { id: 9113, questionKey: 'FB_AMO_013', text: '¿Tu familia tiene conversaciones profundas sobre sueños, miedos y propósito de vida?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'FASE_PILLAR', pillarName: 'amor', milestoneCode: 'M9', active: true, severityWeight: 2 },
  { id: 9114, questionKey: 'FB_AMO_014', text: '¿Los miembros de tu familia sienten que pueden ser completamente auténticos entre sí?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'FASE_PILLAR', pillarName: 'amor', milestoneCode: 'M12', active: true, severityWeight: 2 },
  { id: 9115, questionKey: 'FB_AMO_015', text: '¿Tu familia perdona con genuinidad y no guarda rencores prolongados?', dimension: 'emociones', area: 'EMOCIONES', type: 'FASE_PILLAR', pillarName: 'amor', milestoneCode: 'M12', active: true, severityWeight: 2 },
  { id: 9116, questionKey: 'FB_AMO_016', text: '¿Se promueve en tu hogar el crecimiento personal de cada miembro como parte del amor familiar?', dimension: 'habitos', area: 'HABITOS', type: 'FASE_PILLAR', pillarName: 'amor', milestoneCode: 'M12', active: true, severityWeight: 2 },
  { id: 9117, questionKey: 'FB_AMO_017', text: '¿Tu familia tiene formas de reconocer y reparar el daño emocional cuando ocurre?', dimension: 'emociones', area: 'EMOCIONES', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M6', active: true, severityWeight: 2 },
  { id: 9118, questionKey: 'FB_AMO_018', text: '¿Los miembros de tu familia se apoyan mutuamente en sus metas individuales?', dimension: 'habitos', area: 'HABITOS', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M9', active: true, severityWeight: 1 },
  { id: 9119, questionKey: 'FB_AMO_019', text: '¿Tu familia dedica tiempo intencionado a fortalecer la conexión emocional, más allá de la rutina?', dimension: 'tiempos', area: 'TIEMPOS', type: 'ADAPTIVE', pillarName: 'amor', milestoneCode: 'M9', active: true, severityWeight: 1 },
  { id: 9120, questionKey: 'FB_AMO_020', text: '¿Sientes que el amor de tu familia es incondicional, incluso en los momentos más difíciles?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'amor', milestoneCode: 'M12', active: true, severityWeight: 2 },
];

const ENTREGA: Question[] = [
  { id: 9201, questionKey: 'FB_ENT_001', text: '¿Tu familia tiene una visión compartida de hacia dónde quiere llegar como núcleo familiar?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M15', active: true, severityWeight: 2 },
  { id: 9202, questionKey: 'FB_ENT_002', text: '¿Cada miembro de la familia conoce y se compromete activamente con el propósito familiar?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M15', active: true, severityWeight: 2 },
  { id: 9203, questionKey: 'FB_ENT_003', text: '¿Tu familia contribuye de manera significativa a su comunidad o entorno social?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M18', active: true, severityWeight: 2 },
  { id: 9204, questionKey: 'FB_ENT_004', text: '¿Los valores familiares se viven activamente en el día a día, no solo se declaran?', dimension: 'habitos', area: 'HABITOS', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M18', active: true, severityWeight: 2 },
  { id: 9205, questionKey: 'FB_ENT_005', text: '¿Tu familia transmite activamente sus valores y legado a las generaciones más jóvenes?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M24', active: true, severityWeight: 2 },
  { id: 9206, questionKey: 'FB_ENT_006', text: '¿Existe en tu familia un sentido claro de misión que trasciende el bienestar inmediato?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M24', active: true, severityWeight: 2 },
  { id: 9207, questionKey: 'FB_ENT_007', text: '¿Tu familia invierte tiempo en causas o proyectos que benefician a otros más allá del hogar?', dimension: 'tiempos', area: 'TIEMPOS', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M18', active: true, severityWeight: 1 },
  { id: 9208, questionKey: 'FB_ENT_008', text: '¿Los miembros de la familia modelan comportamientos de servicio y generosidad para los más jóvenes?', dimension: 'habitos', area: 'HABITOS', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M21', active: true, severityWeight: 1 },
  { id: 9209, questionKey: 'FB_ENT_009', text: '¿Tu familia celebra y reconoce los actos de servicio y entrega de cada miembro?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M21', active: true, severityWeight: 1 },
  { id: 9210, questionKey: 'FB_ENT_010', text: '¿Existe un legado familiar documentado (historias, valores, tradiciones) que se comparte activamente?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M24', active: true, severityWeight: 1 },
  { id: 9211, questionKey: 'FB_ENT_011', text: '¿Tu familia toma decisiones importantes considerando el impacto en generaciones futuras?', dimension: 'habitos', area: 'HABITOS', type: 'FASE_PILLAR', pillarName: 'entrega', milestoneCode: 'M30', active: true, severityWeight: 2 },
  { id: 9212, questionKey: 'FB_ENT_012', text: '¿Los miembros de tu familia tienen claridad sobre el aporte único que cada uno hace al mundo?', dimension: 'emociones', area: 'EMOCIONES', type: 'FASE_PILLAR', pillarName: 'entrega', milestoneCode: 'M30', active: true, severityWeight: 2 },
  { id: 9213, questionKey: 'FB_ENT_013', text: '¿Tu familia vive con gratitud activa, reconociendo y celebrando sus bendiciones constantemente?', dimension: 'emociones', area: 'EMOCIONES', type: 'FASE_PILLAR', pillarName: 'entrega', milestoneCode: 'M36', active: true, severityWeight: 2 },
  { id: 9214, questionKey: 'FB_ENT_014', text: '¿Existe un plan concreto para que el legado familiar continúe más allá de la generación actual?', dimension: 'habitos', area: 'HABITOS', type: 'FASE_PILLAR', pillarName: 'entrega', milestoneCode: 'M36', active: true, severityWeight: 2 },
  { id: 9215, questionKey: 'FB_ENT_015', text: '¿Tu familia ha superado adversidades importantes y las usa como fuente de fortaleza compartida?', dimension: 'emociones', area: 'EMOCIONES', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M15', active: true, severityWeight: 2 },
  { id: 9216, questionKey: 'FB_ENT_016', text: '¿Los valores de servicio y entrega son discutidos abiertamente en conversaciones familiares?', dimension: 'comunicacion', area: 'COMUNICACION', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M15', active: true, severityWeight: 1 },
  { id: 9217, questionKey: 'FB_ENT_017', text: '¿Tu familia dedica tiempo intencionado para reflexionar sobre su propósito y misión colectiva?', dimension: 'tiempos', area: 'TIEMPOS', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M18', active: true, severityWeight: 1 },
  { id: 9218, questionKey: 'FB_ENT_018', text: '¿Cada miembro siente que vive una vida con sentido y que su familia lo respalda en eso?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M21', active: true, severityWeight: 2 },
  { id: 9219, questionKey: 'FB_ENT_019', text: '¿Tu familia tiene tradiciones de servicio que se practican regularmente (voluntariados, apoyo a vecinos)?', dimension: 'tiempos', area: 'TIEMPOS', type: 'ADAPTIVE', pillarName: 'entrega', milestoneCode: 'M24', active: true, severityWeight: 1 },
  { id: 9220, questionKey: 'FB_ENT_020', text: '¿Sientes que tu familia está construyendo un legado del que todos se sentirán orgullosos?', dimension: 'emociones', area: 'EMOCIONES', type: 'CORE', pillarName: 'entrega', milestoneCode: 'M36', active: true, severityWeight: 2 },
];

/** Shufflea un array in-place */
function shuffle<T>(arr: T[]): T[] {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/** Devuelve 20 preguntas locales para el pilar dado. Instantáneo, sin red. */
export function getFallbackQuestions(pillar?: string): Question[] {
  let pool: Question[];
  switch ((pillar ?? '').toLowerCase()) {
    case 'amor':    pool = [...AMOR];    break;
    case 'entrega': pool = [...ENTREGA]; break;
    default:        pool = [...REC];     break;
  }
  return shuffle(pool).slice(0, 20);
}
